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
                int q = (today.getMonthValue() - 1) / 3;
                int startMonth = q * 3 + 1;
                LocalDate first = LocalDate.of(today.getYear(), startMonth, 1);
                yield new LocalDateTime[]{first.atStartOfDay(), first.plusMonths(3).atStartOfDay()};
            }
            case "THIS_YEAR" -> {
                LocalDate first = LocalDate.of(today.getYear(), 1, 1);
                yield new LocalDateTime[]{first.atStartOfDay(), first.plusYears(1).atStartOfDay()};
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

        // 1. KPI Aggregation (Sales, Purchases, Inventory Value)
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal purchases = BigDecimal.ZERO;
        
        // aggregate daily sums for the window
        List<Object[]> dailyStats = transactionRepository.dailyStatsBetween(since, until);
        for (Object[] row : dailyStats) {
            BigDecimal sales = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            BigDecimal total = row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2];
            revenue = revenue.add(sales);
            // Purchases approximated as total - sales (includes returns, but works as a conservative inward cost proxy)
            purchases = purchases.add(total.subtract(sales));
        }

        BigDecimal grossProfit = revenue.subtract(purchases);
        int grossMargin = revenue.signum() == 0 ? 0 : grossProfit.multiply(BigDecimal.valueOf(100))
                .divide(revenue, java.math.RoundingMode.HALF_UP).intValue();

        BigDecimal inventoryValue = productRepository.sumInventoryValue();
        if (inventoryValue == null) inventoryValue = BigDecimal.ZERO;

        // 2. Trend Chart (Adaptive Window)
        // If THIS_YEAR, show from Jan 1st. Otherwise show last 6 months to give context.
        LocalDate startOfTrend = range.equalsIgnoreCase("THIS_YEAR") 
                ? LocalDate.now().withDayOfMonth(1).withMonth(1) 
                : LocalDate.now().withDayOfMonth(1).minusMonths(5);
        LocalDateTime trendSince = startOfTrend.atStartOfDay();
        
        // For trend until, we show up to the end of the current selection's window or at least current month
        LocalDateTime trendUntil = until.isAfter(LocalDateTime.now()) ? until : LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay();

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] r : transactionRepository.monthlySalesVsPurchasesBetween(trendSince, trendUntil)) {
            LocalDateTime monthStart = ((java.sql.Timestamp) r[0]).toLocalDateTime();
            BigDecimal sales = r[1] == null ? BigDecimal.ZERO : (BigDecimal) r[1];
            BigDecimal pur = r[2] == null ? BigDecimal.ZERO : (BigDecimal) r[2];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", monthLabel(monthStart));
            m.put("sales", sales);
            m.put("purchases", pur);
            trend.add(m);
        }

        // 3. Category & Turnover performance
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

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Object[]> byProduct = transactionRepository.unitsSoldPurchasedByProductBetween(since, until);
        double totalUnitsSold = 0;
        for (Object[] r : byProduct) {
            Long pid = ((Number) r[0]).longValue();
            String pname = (String) r[1];
            int sold = ((Number) r[2]).intValue();
            int purchasedUnits = ((Number) r[3]).intValue();
            totalUnitsSold += sold;

            Product p = productRepository.findById(pid).orElse(null);
            int closing = p == null || p.getStockQuantity() == null ? 0 : p.getStockQuantity();
            double rate = closing <= 0 ? sold : ((double) sold / (double) closing);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("product", pname);
            m.put("unitsSold", sold);
            m.put("unitsPurchased", purchasedUnits);
            m.put("closingStock", closing);
            m.put("turnoverRate", Math.round(rate * 10.0) / 10.0);
            rows.add(m);
        }

        long productCount = productRepository.count();
        double avgStock = 0;
        if (productCount > 0) {
            double sumStock = productRepository.findAll().stream()
                    .mapToDouble(p -> p.getStockQuantity() == null ? 0 : p.getStockQuantity())
                    .sum();
            avgStock = sumStock / productCount;
        }
        double turnoverRate = avgStock <= 0 ? 0.0 : (totalUnitsSold / avgStock);

        // 4. Supplier Analytics
        List<Map<String, Object>> analyticsSuppliers = new ArrayList<>();
        supplierMetricsRepository.findAll().forEach(sm -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("supplier", sm.getSupplier() != null ? sm.getSupplier().getName() : "—");
            m.put("rating", sm.getStarRating() == null ? 0.0 : sm.getStarRating());
            analyticsSuppliers.add(m);
        });

        // 5. Dynamic Insights
        String periodName = range.replace("_", " ").toLowerCase();
        String insight = String.format("Performance insight for %s: ", periodName);
        if (revenue.compareTo(purchases) > 0) {
            insight += "Positive cash flow detected. Revenue exceeds procurement costs by " + formatMoney(revenue.subtract(purchases)) + ".";
        } else if (revenue.signum() > 0) {
            insight += "Procurement heavy period. Inventory investment is currently higher than conversion.";
        } else {
            insight += "No significant transaction volume detected in this period.";
        }

        return Response.builder()
                .status(200)
                .message("success")
                .revenue(revenue)
                .grossProfit(grossProfit)
                .grossMargin(grossMargin)
                .inventoryValue(inventoryValue)
                .turnoverRate(Math.round(turnoverRate * 10.0) / 10.0)
                .industryAvg(2.8)
                .trend(trend)
                .analyticsCategories(analyticsCategories)
                .rows(rows)
                .analyticsSuppliers(analyticsSuppliers)
                .insight(insight)
                .build();
    }

    private String formatMoney(BigDecimal val) {
        return "₹" + String.format("%,.0f", val);
    }
}
