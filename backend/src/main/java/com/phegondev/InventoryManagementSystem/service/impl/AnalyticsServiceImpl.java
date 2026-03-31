package com.phegondev.InventoryManagementSystem.service.impl;

import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.entity.Product;
import com.phegondev.InventoryManagementSystem.repository.ProductRepository;
import com.phegondev.InventoryManagementSystem.repository.SupplierMetricsRepository;
import com.phegondev.InventoryManagementSystem.repository.TransactionRepository;
import com.phegondev.InventoryManagementSystem.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final SupplierMetricsRepository supplierMetricsRepository;

    private static LocalDateTime[] rangeToWindow(String range) {
        LocalDate today = LocalDate.now();
        return switch (String.valueOf(range).toUpperCase(Locale.ROOT)) {
            case "LAST_MONTH" -> {
                LocalDate first = today.minusMonths(1).withDayOfMonth(1);
                yield new LocalDateTime[]{first.atStartOfDay(), first.plusMonths(1).atStartOfDay()};
            }
            case "THIS_QUARTER" -> {
                int q = (today.getMonthValue() - 1) / 3; // 0..3
                int startMonth = q * 3 + 1;
                LocalDate first = LocalDate.of(today.getYear(), startMonth, 1);
                yield new LocalDateTime[]{first.atStartOfDay(), first.plusMonths(3).atStartOfDay()};
            }
            case "THIS_YEAR" -> {
                LocalDate first = LocalDate.of(today.getYear(), 1, 1);
                yield new LocalDateTime[]{first.atStartOfDay(), first.plusYears(1).atStartOfDay()};
            }
            case "THIS_MONTH" -> {
                LocalDate first = today.withDayOfMonth(1);
                yield new LocalDateTime[]{first.atStartOfDay(), first.plusMonths(1).atStartOfDay()};
            }
            default -> {
                LocalDate first = today.withDayOfMonth(1);
                yield new LocalDateTime[]{first.atStartOfDay(), first.plusMonths(1).atStartOfDay()};
            }
        };
    }

    private static String monthLabel(LocalDateTime dt) {
        Month m = dt.getMonth();
        return m.getDisplayName(TextStyle.SHORT, Locale.US) + " " + dt.getYear();
    }

    @Override
    @Transactional(readOnly = true)
    public Response summary(String range) {
        LocalDateTime[] win = rangeToWindow(range);
        LocalDateTime since = win[0];
        LocalDateTime until = win[1];

        // Revenue (sales) + purchases in range.
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal purchases = BigDecimal.ZERO;
        for (Object[] row : transactionRepository.sumByTypeSince(since)) {
            // this query is "since" only; we'll compute by filtering trend window for KPI accuracy using dailyStatsBetween
        }
        // KPI accuracy via dailyStatsBetween (windowed)
        for (Object[] row : transactionRepository.dailyStatsBetween(since, until)) {
            // row: date, sales_sum, total_sum, count
            BigDecimal sales = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            BigDecimal total = row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2];
            revenue = revenue.add(sales);
            // purchases approximated: total - sales (since total includes all types)
            purchases = purchases.add(total.subtract(sales));
        }

        // Gross profit (simple): sales - purchases (in same window)
        BigDecimal grossProfit = revenue.subtract(purchases);
        int grossMargin = revenue.signum() == 0 ? 0 : grossProfit.multiply(BigDecimal.valueOf(100))
                .divide(revenue, java.math.RoundingMode.HALF_UP).intValue();

        BigDecimal inventoryValue = productRepository.sumInventoryValue();
        if (inventoryValue == null) inventoryValue = BigDecimal.ZERO;

        // Turnover (simple proxy): units sold / average stock (approx)
        double totalUnitsSold = 0;
        List<Object[]> byProduct = transactionRepository.unitsSoldPurchasedByProductBetween(since, until);
        for (Object[] r : byProduct) {
            totalUnitsSold += ((Number) r[2]).doubleValue();
        }
        long productCount = productRepository.count();
        double avgStock = 0;
        if (productCount > 0) {
            List<Product> all = productRepository.findAll();
            double sumStock = 0;
            for (Product p : all) sumStock += (p.getStockQuantity() == null ? 0 : p.getStockQuantity());
            avgStock = sumStock / productCount;
        }
        double turnoverRate = avgStock <= 0 ? 0.0 : (totalUnitsSold / avgStock);
        double industryAvg = 2.8;

        // Trend: last 6 months side-by-side bars.
        LocalDateTime trendSince = LocalDate.now().withDayOfMonth(1).minusMonths(5).atStartOfDay();
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] r : transactionRepository.monthlySalesVsPurchasesSince(trendSince)) {
            LocalDateTime monthStart = ((java.sql.Timestamp) r[0]).toLocalDateTime();
            BigDecimal sales = r[1] == null ? BigDecimal.ZERO : (BigDecimal) r[1];
            BigDecimal pur = r[2] == null ? BigDecimal.ZERO : (BigDecimal) r[2];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", monthLabel(monthStart));
            m.put("sales", sales);
            m.put("purchases", pur);
            trend.add(m);
        }

        // Category performance (revenue + percent)
        List<Object[]> catRows = transactionRepository.revenueByCategoryBetween(since, until);
        BigDecimal totalCatRevenue = BigDecimal.ZERO;
        for (Object[] r : catRows) {
            totalCatRevenue = totalCatRevenue.add(r[1] == null ? BigDecimal.ZERO : (BigDecimal) r[1]);
        }
        List<Map<String, Object>> analyticsCategories = new ArrayList<>();
        for (Object[] r : catRows) {
            String name = (String) r[0];
            BigDecimal rev = r[1] == null ? BigDecimal.ZERO : (BigDecimal) r[1];
            int pct = totalCatRevenue.signum() == 0 ? 0 : rev.multiply(BigDecimal.valueOf(100))
                    .divide(totalCatRevenue, java.math.RoundingMode.HALF_UP).intValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", name);
            m.put("revenue", rev);
            m.put("percent", pct);
            analyticsCategories.add(m);
        }

        // Inventory turnover table rows
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : byProduct) {
            Long pid = ((Number) r[0]).longValue();
            String pname = (String) r[1];
            int sold = ((Number) r[2]).intValue();
            int purchasedUnits = ((Number) r[3]).intValue();
            Product p = productRepository.findById(pid).orElse(null);
            int closing = p == null || p.getStockQuantity() == null ? 0 : p.getStockQuantity();
            double rate = closing <= 0 ? sold : ((double) sold / (double) closing);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("product", pname);
            m.put("unitsSold", sold);
            m.put("unitsPurchased", purchasedUnits);
            m.put("closingStock", closing);
            m.put("turnoverRate", Math.round(rate * 10.0) / 10.0);
            m.put("trend", "→");
            rows.add(m);
        }

        // Supplier performance analytics (star rating bars)
        List<Map<String, Object>> analyticsSuppliers = new ArrayList<>();
        supplierMetricsRepository.findAll().forEach(sm -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("supplier", sm.getSupplier() != null ? sm.getSupplier().getName() : "—");
            m.put("rating", sm.getStarRating() == null ? 0.0 : sm.getStarRating());
            analyticsSuppliers.add(m);
        });

        String insight = "Analytics Insight: Inventory turnover improved this month. Review high-velocity categories and reorder fast-moving SKUs.";

        return Response.builder()
                .status(200)
                .message("success")
                .revenue(revenue)
                .grossProfit(grossProfit)
                .grossMargin(grossMargin)
                .inventoryValue(inventoryValue)
                .turnoverRate(Math.round(turnoverRate * 10.0) / 10.0)
                .industryAvg(industryAvg)
                .trend(trend)
                .analyticsCategories(analyticsCategories)
                .rows(rows)
                .analyticsSuppliers(analyticsSuppliers)
                .insight(insight)
                .build();
    }
}

