package com.phegondev.InventoryManagementSystem.service.impl;

import com.phegondev.InventoryManagementSystem.dto.DashboardSummaryDTO;
import com.phegondev.InventoryManagementSystem.dto.ProductDTO;
import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.dto.TransactionDTO;
import com.phegondev.InventoryManagementSystem.entity.Product;
import com.phegondev.InventoryManagementSystem.entity.Transaction;
import com.phegondev.InventoryManagementSystem.enums.TransactionType;
import com.phegondev.InventoryManagementSystem.enums.UserRole;
import com.phegondev.InventoryManagementSystem.repository.CategoryRepository;
import com.phegondev.InventoryManagementSystem.repository.ProductRepository;
import com.phegondev.InventoryManagementSystem.repository.SupplierRepository;
import com.phegondev.InventoryManagementSystem.repository.TransactionRepository;
import com.phegondev.InventoryManagementSystem.service.DashboardService;
import com.phegondev.InventoryManagementSystem.service.TransactionTimeSeriesHelper;
import com.phegondev.InventoryManagementSystem.service.UserService;
import com.phegondev.InventoryManagementSystem.entity.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.phegondev.InventoryManagementSystem.dto.ChartPointDTO;
import java.sql.Date;
import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final TransactionRepository transactionRepository;
    private final ModelMapper modelMapper;
    private final TransactionTimeSeriesHelper timeSeriesHelper;
    private final UserService userService;

    @Value("${ims.low-stock-threshold:10}")
    private int lowStockThreshold;

    @Override
    @Transactional(readOnly = true)
    public Response getSummary() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        BigDecimal inventoryValue = productRepository.sumInventoryValue();
        if (inventoryValue == null) {
            inventoryValue = BigDecimal.ZERO;
        }

        List<Product> lowStockEntities = productRepository.findByStockQuantityLessThanOrderByStockQuantityAsc(
                lowStockThreshold,
                PageRequest.of(0, 10));
        List<ProductDTO> lowStockProducts = modelMapper.map(lowStockEntities, new TypeToken<List<ProductDTO>>() {
        }.getType());
        for (int i = 0; i < lowStockEntities.size(); i++) {
            Product p = lowStockEntities.get(i);
            if (p.getCategory() != null) {
                lowStockProducts.get(i).setCategoryId(p.getCategory().getId());
            }
        }

        var recentPage = transactionRepository.findAll(
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<TransactionDTO> recentDtos = modelMapper.map(recentPage.getContent(),
                new TypeToken<List<TransactionDTO>>() {
                }.getType());
        for (int i = 0; i < recentPage.getContent().size(); i++) {
            Transaction t = recentPage.getContent().get(i);
            TransactionDTO dto = recentDtos.get(i);
            dto.setUser(null);
            dto.setSupplier(null);
            if (t.getProduct() != null) {
                ProductDTO pd = modelMapper.map(t.getProduct(), ProductDTO.class);
                pd.setCategoryId(t.getProduct().getCategory() != null ? t.getProduct().getCategory().getId() : null);
                dto.setProduct(pd);
            }
        }

        User currentUser = userService.getCurrentLoggedInUser();
        UserRole role = currentUser.getRole();
        var seven = timeSeriesHelper.buildSevenDayWindow(today);

        DashboardSummaryDTO.DashboardSummaryDTOBuilder summaryBuilder = DashboardSummaryDTO.builder()
                .lowStockThreshold(lowStockThreshold);

        if (role == UserRole.ADMIN || role == UserRole.WAREHOUSE_MANAGER || role == UserRole.PROCUREMENT_OFFICER || role == UserRole.ANALYST) {
            summaryBuilder.inventoryValue(inventoryValue)
                    .lowStockProductCount(productRepository.countByStockQuantityLessThan(lowStockThreshold))
                    .lowStockProducts(lowStockProducts)
                    .recentTransactions(recentDtos)
                    .insightCards(timeSeriesHelper.buildDashboardInsightCards(inventoryValue))
                    .sevenDayVolumeBars(timeSeriesHelper.toBarPoints(seven))
                    .totalProducts(productRepository.count())
                    .totalCategories(categoryRepository.count())
                    .totalSuppliers(supplierRepository.count())
                    .salesRevenueThisMonth(nullToZero(
                            transactionRepository.sumTotalPriceByTypeAndMonth(TransactionType.SALE, year, month)))
                    .purchaseTransactionsThisMonth(
                            transactionRepository.countByTypeAndMonth(TransactionType.PURCHASE, year, month))
                    .saleTransactionsThisMonth(
                            transactionRepository.countByTypeAndMonth(TransactionType.SALE, year, month));

            if (role == UserRole.ADMIN) {
                summaryBuilder.sevenDayVolumeLine(timeSeriesHelper.toLinePoints(seven))
                        .categoryInventoryShare(timeSeriesHelper.categoryInventoryShareBars(6))
                        .activityDonutLast30Days(timeSeriesHelper.donutByTypeLast30Days());
            }
        } else if (role == UserRole.STAFF) {
            summaryBuilder
                    .mySalesToday(nullToZero(transactionRepository
                            .sumTotalPriceByUserIdAndTypeAndDate(currentUser.getId(), TransactionType.SALE, today)))
                    .myTransactionsCount(transactionRepository.countByUserId(currentUser.getId()));

            // Filter recent for staff
            recentDtos.removeIf(d -> {
                Transaction t = transactionRepository.findById(d.getId()).orElse(null);
                return t != null && !t.getUser().getId().equals(currentUser.getId());
            });
            summaryBuilder.recentTransactions(recentDtos);

            // Fetch Staff Sales Graphs (last 12 months and last 30 days)
            LocalDateTime twelveMonthsAgo = today.minusMonths(11).withDayOfMonth(1).atStartOfDay();
            List<Object[]> monthlyRows = transactionRepository.sumSalesByUserIdLastMonths(currentUser.getId(), twelveMonthsAgo);
            summaryBuilder.mySalesLast12Months(fillMonthlyGaps(monthlyRows, twelveMonthsAgo, 12));

            LocalDateTime thirtyDaysAgo = today.minusDays(29).atStartOfDay();
            List<Object[]> dailyRows = transactionRepository.sumSalesByUserIdLastDays(currentUser.getId(), thirtyDaysAgo);
            summaryBuilder.mySalesLast30Days(fillDailyGaps(dailyRows, thirtyDaysAgo.toLocalDate(), 30));
        }

        DashboardSummaryDTO summary = summaryBuilder.build();

        return Response.builder()
                .status(200)
                .message("success")
                .dashboardSummary(summary)
                .build();
    }

    private List<ChartPointDTO> fillMonthlyGaps(List<Object[]> rows, LocalDateTime start, int months) {
        Map<LocalDate, BigDecimal> dataMap = new HashMap<>();
        for (Object[] row : rows) {
            // DATE_TRUNC returns a Timestamp or Date
            LocalDate d = row[0] instanceof Timestamp ? ((Timestamp) row[0]).toLocalDateTime().toLocalDate() 
                         : ((java.util.Date) row[0]).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            dataMap.put(d.withDayOfMonth(1), (BigDecimal) row[1]);
        }
        List<ChartPointDTO> result = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            LocalDate current = start.plusMonths(i).toLocalDate().withDayOfMonth(1);
            String label = current.getMonth().getDisplayName(TextStyle.SHORT, Locale.US) + " " + (current.getYear() % 100);
            result.add(new ChartPointDTO(label, dataMap.getOrDefault(current, BigDecimal.ZERO)));
        }
        return result;
    }

    private List<ChartPointDTO> fillDailyGaps(List<Object[]> rows, LocalDate start, int days) {
        Map<LocalDate, BigDecimal> dataMap = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate d = row[0] instanceof Date ? ((Date) row[0]).toLocalDate() : (LocalDate) row[0];
            dataMap.put(d, (BigDecimal) row[1]);
        }
        List<ChartPointDTO> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate current = start.plusDays(i);
            String label = current.getDayOfMonth() + " " + current.getMonth().getDisplayName(TextStyle.SHORT, Locale.US);
            result.add(new ChartPointDTO(label, dataMap.getOrDefault(current, BigDecimal.ZERO)));
        }
        return result;
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
