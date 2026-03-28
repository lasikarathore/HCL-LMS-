package com.phegondev.InventoryManagementSystem.service.impl;

import com.phegondev.InventoryManagementSystem.dto.DashboardSummaryDTO;
import com.phegondev.InventoryManagementSystem.dto.ProductDTO;
import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.dto.TransactionDTO;
import com.phegondev.InventoryManagementSystem.entity.Product;
import com.phegondev.InventoryManagementSystem.entity.Transaction;
import com.phegondev.InventoryManagementSystem.enums.TransactionType;
import com.phegondev.InventoryManagementSystem.repository.CategoryRepository;
import com.phegondev.InventoryManagementSystem.repository.ProductRepository;
import com.phegondev.InventoryManagementSystem.repository.SupplierRepository;
import com.phegondev.InventoryManagementSystem.repository.TransactionRepository;
import com.phegondev.InventoryManagementSystem.service.DashboardService;
import com.phegondev.InventoryManagementSystem.service.TransactionTimeSeriesHelper;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final TransactionRepository transactionRepository;
    private final ModelMapper modelMapper;
    private final TransactionTimeSeriesHelper timeSeriesHelper;

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
                PageRequest.of(0, 10)
        );
        List<ProductDTO> lowStockProducts = modelMapper.map(lowStockEntities, new TypeToken<List<ProductDTO>>() {}.getType());
        for (int i = 0; i < lowStockEntities.size(); i++) {
            Product p = lowStockEntities.get(i);
            if (p.getCategory() != null) {
                lowStockProducts.get(i).setCategoryId(p.getCategory().getId());
            }
        }

        var recentPage = transactionRepository.findAll(
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<TransactionDTO> recentDtos = modelMapper.map(recentPage.getContent(), new TypeToken<List<TransactionDTO>>() {}.getType());
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

        var seven = timeSeriesHelper.buildSevenDayWindow(LocalDate.now());

        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .totalProducts(productRepository.count())
                .totalCategories(categoryRepository.count())
                .totalSuppliers(supplierRepository.count())
                .inventoryValue(inventoryValue)
                .lowStockProductCount(productRepository.countByStockQuantityLessThan(lowStockThreshold))
                .lowStockThreshold(lowStockThreshold)
                .salesRevenueThisMonth(nullToZero(transactionRepository.sumTotalPriceByTypeAndMonth(TransactionType.SALE, year, month)))
                .purchaseTransactionsThisMonth(transactionRepository.countByTypeAndMonth(TransactionType.PURCHASE, year, month))
                .saleTransactionsThisMonth(transactionRepository.countByTypeAndMonth(TransactionType.SALE, year, month))
                .lowStockProducts(lowStockProducts)
                .recentTransactions(recentDtos)
                .insightCards(timeSeriesHelper.buildDashboardInsightCards(inventoryValue))
                .sevenDayVolumeBars(timeSeriesHelper.toBarPoints(seven))
                .sevenDayVolumeLine(timeSeriesHelper.toLinePoints(seven))
                .categoryInventoryShare(timeSeriesHelper.categoryInventoryShareBars(6))
                .activityDonutLast30Days(timeSeriesHelper.donutByTypeLast30Days())
                .build();

        return Response.builder()
                .status(200)
                .message("success")
                .dashboardSummary(summary)
                .build();
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
