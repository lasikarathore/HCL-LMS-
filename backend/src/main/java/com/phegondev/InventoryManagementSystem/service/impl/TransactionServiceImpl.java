package com.phegondev.InventoryManagementSystem.service.impl;

import com.phegondev.InventoryManagementSystem.dto.ChartPointDTO;
import com.phegondev.InventoryManagementSystem.dto.ChartSeriesGroupDTO;
import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.dto.TransactionAnalyticsDTO;
import com.phegondev.InventoryManagementSystem.dto.TransactionDTO;
import com.phegondev.InventoryManagementSystem.dto.TransactionRequest;
import com.phegondev.InventoryManagementSystem.entity.Product;
import com.phegondev.InventoryManagementSystem.entity.Supplier;
import com.phegondev.InventoryManagementSystem.entity.Transaction;
import com.phegondev.InventoryManagementSystem.entity.User;
import com.phegondev.InventoryManagementSystem.enums.TransactionStatus;
import com.phegondev.InventoryManagementSystem.enums.TransactionType;
import com.phegondev.InventoryManagementSystem.enums.UserRole;
import com.phegondev.InventoryManagementSystem.exceptions.NameValueRequiredException;
import com.phegondev.InventoryManagementSystem.exceptions.NotFoundException;
import com.phegondev.InventoryManagementSystem.repository.ProductRepository;
import com.phegondev.InventoryManagementSystem.repository.SupplierRepository;
import com.phegondev.InventoryManagementSystem.repository.TransactionRepository;
import com.phegondev.InventoryManagementSystem.service.StockAlertService;
import com.phegondev.InventoryManagementSystem.service.TransactionService;
import com.phegondev.InventoryManagementSystem.service.TransactionTimeSeriesHelper;
import com.phegondev.InventoryManagementSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phegondev.InventoryManagementSystem.repository.SupplierProfileRepository;
import com.phegondev.InventoryManagementSystem.exceptions.ConflictException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final ModelMapper modelMapper;
    private final SupplierRepository supplierRepository;
    private final SupplierProfileRepository supplierProfileRepository;
    private final UserService userService;
    private final ProductRepository productRepository;
    private final TransactionTimeSeriesHelper timeSeriesHelper;
    private final StockAlertService stockAlertService;



    @Override
    @Transactional
    public Response restockInventory(TransactionRequest transactionRequest) {

        Long productId = transactionRequest.getProductId();
        Long supplierId = transactionRequest.getSupplierId();
        Integer quantity = transactionRequest.getQuantity();

        if (supplierId == null) throw new NameValueRequiredException("Supplier Id id Required");

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new NotFoundException("Product Not Found"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        // Check if supplier is active via profile
        supplierProfileRepository.findBySupplier_Id(supplier.getId()).ifPresent(profile -> {
            if (profile.getActive() != null && !profile.getActive()) {
                throw new ConflictException(
                        "Supplier '" + supplier.getName() + "' is currently INACTIVE and cannot be used for restock.");
            }
        });

        User user = userService.getCurrentLoggedInUser();

        //update the stock quantity and re-save
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
        stockAlertService.reconcileAfterStockChange(product);

        //create a transaction
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.PURCHASE)
                // Keep null to stay compatible with existing DB status check constraint values.
                .status(null)
                .product(product)
                .user(user)
                .supplier(supplier)
                .totalProducts(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .description(transactionRequest.getDescription())
                .build();

        transactionRepository.save(transaction);

        return Response.builder()
                .status(200)
                .message("Transaction Made Successfully")
                .build();



    }

    @Override
    @Transactional
    public Response sell(TransactionRequest transactionRequest) {

        Long productId = transactionRequest.getProductId();
        Integer quantity = transactionRequest.getQuantity();


        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new NotFoundException("Product Not Found"));


        User user = userService.getCurrentLoggedInUser();

        // Validate stock availability
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }

        //update the stock quantity and re-save
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
        stockAlertService.reconcileAfterStockChange(product);

        //create a transaction
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.SALE)
                // Keep null to stay compatible with existing DB status check constraint values.
                .status(null)
                .product(product)
                .user(user)
                .totalProducts(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .description(transactionRequest.getDescription())
                .build();

        transactionRepository.save(transaction);

        return Response.builder()
                .status(200)
                .message("Transaction Sold Successfully")
                .build();
    }

    @Override
    public Response returnToSupplier(TransactionRequest transactionRequest) {

        Long productId = transactionRequest.getProductId();
        Long supplierId = transactionRequest.getSupplierId();
        Integer quantity = transactionRequest.getQuantity();

        if (supplierId == null) throw new NameValueRequiredException("Supplier Id id Required");

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new NotFoundException("Product Not Found"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        // Check if supplier is active via profile
        supplierProfileRepository.findBySupplier_Id(supplier.getId()).ifPresent(profile -> {
            if (profile.getActive() != null && !profile.getActive()) {
                throw new ConflictException(
                        "Supplier '" + supplier.getName() + "' is currently INACTIVE and cannot be used for returns.");
            }
        });

        User user = userService.getCurrentLoggedInUser();

        //update the stock quantity and re-save
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
        stockAlertService.reconcileAfterStockChange(product);

        //create a transaction
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.RETURN_TO_SUPPLIER)
                // Keep null to stay compatible with existing DB status check constraint values.
                .status(null)
                .product(product)
                .user(user)
                .supplier(supplier)
                .totalProducts(quantity)
                .totalPrice(BigDecimal.ZERO)
                .description(transactionRequest.getDescription())
                .build();

        transactionRepository.save(transaction);

        return Response.builder()
                .status(200)
                .message("Transaction Returned Successfully Initialized")
                .build();
    }

    @Override
    public Response getAllTransactions(int page, int size, String searchText, TransactionType transactionType, String status) {
        User current = userService.getCurrentLoggedInUser();
        Long userId = (current.getRole() == UserRole.STAFF) ? current.getId() : null;

        // Native search query orders by id in SQL; avoid extra ORDER BY from Pageable (invalid on native queries).
        Pageable pageable = PageRequest.of(page, size);
        String st = (searchText == null || searchText.isBlank()) ? null : searchText.trim();
        
        // If no type is specified, we exclude PURCHASE to keep inward history localized to its specific view.
        // If PURCHASE is explicitly requested (e.g. from PurchaseComponent), we use it.
        String tx = transactionType == null ? null : transactionType.name();
        
        String ts = (status == null || status.isBlank()) ? null : status.trim();
        Page<Transaction> transactionPage = transactionRepository.pageTransactionsFiltered(tx, st, ts, userId, pageable);

        List<TransactionDTO> transactionDTOS = modelMapper
                .map(transactionPage.getContent(), new TypeToken<List<TransactionDTO>>() {}.getType());

        transactionDTOS.forEach(transactionDTOItem -> {
            transactionDTOItem.setUser(null);
            transactionDTOItem.setProduct(null);
            transactionDTOItem.setSupplier(null);
        });


        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactionDTOS)
                .totalPages(transactionPage.getTotalPages())
                .totalElements(transactionPage.getTotalElements())
                .build();
    }

    @Override
    public Response getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Transaction Not Found"));

        TransactionDTO transactionDTO = modelMapper.map(transaction, TransactionDTO.class);

        transactionDTO.getUser().setTransactions(null); //removing the user trnasaction list

        return Response.builder()
                .status(200)
                .message("success")
                .transaction(transactionDTO)
                .build();

    }

    @Override
    public Response getAllTransactionByMonthAndYear(int month, int year) {

       List<Transaction> transactions = transactionRepository.findAllByMonthAndYear(month, year);

        List<TransactionDTO> transactionDTOS = modelMapper
                .map(transactions, new TypeToken<List<TransactionDTO>>() {}.getType());

        transactionDTOS.forEach(transactionDTOItem -> {
            transactionDTOItem.setUser(null);
            transactionDTOItem.setProduct(null);
            transactionDTOItem.setSupplier(null);
        });


        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactionDTOS)
                .build();
    }

    @Override
    public Response updateTransactionStatus(Long transactionId, TransactionStatus transactionStatus) {

        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(()-> new NotFoundException("Transaction Not Found"));

        existingTransaction.setStatus(transactionStatus);
        existingTransaction.setUpdatedAt(LocalDateTime.now());

        transactionRepository.save(existingTransaction);

        return Response.builder()
                .status(200)
                .message("Transaction Status Successfully Updated")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getTransactionAnalytics(Integer month, Integer year) {

        List<ChartPointDTO> countByType = new ArrayList<>();
        List<ChartPointDTO> amountByType = new ArrayList<>();
        for (Object[] row : transactionRepository.aggregateCountAndAmountByType()) {
            TransactionType tt = (TransactionType) row[0];
            long cnt = ((Number) row[1]).longValue();
            BigDecimal sum = row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2];
            String name = tt.name();
            countByType.add(ChartPointDTO.builder().name(name).value(BigDecimal.valueOf(cnt)).build());
            amountByType.add(ChartPointDTO.builder().name(name).value(sum).build());
        }

        List<Object[]> monthTypeRows = transactionRepository.aggregateAmountByYearMonthAndType();
        Map<String, Map<String, BigDecimal>> byYm = new LinkedHashMap<>();
        for (Object[] row : monthTypeRows) {
            int y = ((Number) row[0]).intValue();
            int m = ((Number) row[1]).intValue();
            TransactionType tt = (TransactionType) row[2];
            BigDecimal amt = row[3] == null ? BigDecimal.ZERO : (BigDecimal) row[3];
            String key = String.format(Locale.ROOT, "%04d-%02d", y, m);
            byYm.computeIfAbsent(key, k -> new LinkedHashMap<>()).merge(tt.name(), amt, BigDecimal::add);
        }

        List<String> ymKeys = new ArrayList<>(byYm.keySet());
        List<String> last12 = ymKeys.size() <= 12
                ? ymKeys
                : ymKeys.subList(ymKeys.size() - 12, ymKeys.size());

        List<ChartSeriesGroupDTO> monthlyAmountByType = new ArrayList<>();
        List<ChartPointDTO> monthlyTotalVolume = new ArrayList<>();
        for (String key : last12) {
            String label = formatYearMonthLabel(key);
            Map<String, BigDecimal> types = byYm.get(key);
            List<ChartPointDTO> series = new ArrayList<>();
            BigDecimal monthTotal = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> e : types.entrySet()) {
                series.add(ChartPointDTO.builder().name(e.getKey()).value(e.getValue()).build());
                monthTotal = monthTotal.add(e.getValue());
            }
            monthlyAmountByType.add(ChartSeriesGroupDTO.builder().name(label).series(series).build());
            monthlyTotalVolume.add(ChartPointDTO.builder().name(label).value(monthTotal).build());
        }

        List<ChartPointDTO> dailyAmounts = new ArrayList<>();
        if (month != null && year != null && month >= 1 && month <= 12) {
            Map<Integer, BigDecimal> byDay = new TreeMap<>();
            for (Transaction t : transactionRepository.findAllByMonthAndYear(month, year)) {
                int day = t.getCreatedAt().getDayOfMonth();
                BigDecimal price = t.getTotalPrice() == null ? BigDecimal.ZERO : t.getTotalPrice();
                byDay.merge(day, price, BigDecimal::add);
            }
            for (Map.Entry<Integer, BigDecimal> e : byDay.entrySet()) {
                dailyAmounts.add(ChartPointDTO.builder()
                        .name("Day " + e.getKey())
                        .value(e.getValue())
                        .build());
            }
        }

        var seven = timeSeriesHelper.buildSevenDayWindow(java.time.LocalDate.now());

        TransactionAnalyticsDTO analytics = TransactionAnalyticsDTO.builder()
                .countByType(countByType)
                .amountByType(amountByType)
                .dailyAmountsInMonth(dailyAmounts.isEmpty() ? null : dailyAmounts)
                .monthlyAmountByType(monthlyAmountByType)
                .monthlyTotalVolume(monthlyTotalVolume)
                .insightCards(timeSeriesHelper.buildTransactionHubInsightCards())
                .sevenDayVolumeBars(timeSeriesHelper.toBarPoints(seven))
                .sevenDayVolumeLine(timeSeriesHelper.toLinePoints(seven))
                .topProductVolumeShare(timeSeriesHelper.topProductHorizontalBars(6))
                .volumeDonutByTypeLast30Days(timeSeriesHelper.donutByTypeLast30Days())
                .topProductLeaders(timeSeriesHelper.topProductRows(8))
                .build();

        return Response.builder()
                .status(200)
                .message("success")
                .transactionAnalytics(analytics)
                .build();
    }

    private static String formatYearMonthLabel(String yyyyMm) {
        String[] p = yyyyMm.split("-");
        int y = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        return Month.of(m).getDisplayName(TextStyle.SHORT, Locale.US) + " " + y;
    }
}
