package com.phegondev.InventoryManagementSystem.service.impl;

import com.phegondev.InventoryManagementSystem.dto.*;
import com.phegondev.InventoryManagementSystem.enums.TransactionType;
import com.phegondev.InventoryManagementSystem.repository.PurchaseOrderRepository;
import com.phegondev.InventoryManagementSystem.service.DashboardService;
import com.phegondev.InventoryManagementSystem.service.AnalyticsService;
import com.phegondev.InventoryManagementSystem.entity.Product;
import com.phegondev.InventoryManagementSystem.repository.ProductRepository;
import com.phegondev.InventoryManagementSystem.repository.SupplierMetricsRepository;
import com.phegondev.InventoryManagementSystem.repository.TransactionRepository;
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
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DashboardService dashboardService;

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

    @Override
    @Transactional(readOnly = true)
    public Response getBiSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yearStart = now.toLocalDate().withDayOfYear(1).atStartOfDay();
        LocalDateTime lastYearStart = yearStart.minusYears(1);
        LocalDateTime lastYearUntil = now.minusYears(1);

        // 1. Financial KPIs (Current vs Previous Comparison)
        BigDecimal ytdRevenue = transactionRepository.sumTotalPriceByTypeAndSince(TransactionType.SALE, yearStart);
        BigDecimal lytdRevenue = transactionRepository.sumTotalPriceByTypeBetween(TransactionType.SALE, lastYearStart, lastYearUntil);
        
        // Financial KPIs: Use actual margins from historical data
        BigDecimal ytdCogs = BigDecimal.ZERO;
        List<Product> allProducts = productRepository.findAll();
        for (Product p : allProducts) {
            long sold = transactionRepository.aggregateUnitsSoldForProductInPeriod(p.getId(), TransactionType.SALE, yearStart);
            BigDecimal costPrice = purchaseOrderRepository.findLatestPurchasePrice(p.getId());
            if (costPrice == null) costPrice = p.getPrice().multiply(BigDecimal.valueOf(0.6)); // fallback if never purchased
            ytdCogs = ytdCogs.add(BigDecimal.valueOf(sold).multiply(costPrice));
        }

        BigDecimal avgMargin = ytdRevenue.signum() == 0 ? BigDecimal.valueOf(0.3) : 
            ytdRevenue.subtract(ytdCogs).divide(ytdRevenue, 4, java.math.RoundingMode.HALF_UP);
        
        BigDecimal lytdCogs = lytdRevenue.multiply(BigDecimal.ONE.subtract(avgMargin));
        
        BigDecimal ytdProfit = ytdRevenue.subtract(ytdCogs);
        BigDecimal lytdProfit = lytdRevenue.subtract(lytdCogs);

        double netMargin = ytdRevenue.signum() == 0 ? 0.0 : 
            ytdProfit.multiply(BigDecimal.valueOf(100)).divide(ytdRevenue, 2, java.math.RoundingMode.HALF_UP).doubleValue();

        List<BiFinancialKpiDTO> financialStats = List.of(
            new BiFinancialKpiDTO("YTD Revenue", formatMoney(ytdRevenue), calculateTrend(ytdRevenue, lytdRevenue), trendClass(ytdRevenue, lytdRevenue)),
            new BiFinancialKpiDTO("Gross Profit", formatMoney(ytdProfit), calculateTrend(ytdProfit, lytdProfit), trendClass(ytdProfit, lytdProfit)),
            new BiFinancialKpiDTO("COGS", formatMoney(ytdCogs), calculateTrend(ytdCogs, lytdCogs), trendClassInv(ytdCogs, lytdCogs)),
            new BiFinancialKpiDTO("Net Margin", netMargin + "%", "Real-time", "trend-up")
        );

        // 2. Supply Chain Metrics
        Double leadTime = purchaseOrderRepository.avgLeadTimeDays();
        double otif = supplierMetricsRepository.avgOnTime();
        
        // Stock Turnover = Total Units Sold YTD / Average Stock
        long unitsSoldYtd = 0;
        for (Product p : allProducts) {
            unitsSoldYtd += transactionRepository.aggregateUnitsSoldForProductInPeriod(p.getId(), TransactionType.SALE, yearStart);
        }
        long currentStock = allProducts.stream().mapToLong(p -> p.getStockQuantity() == null ? 0 : p.getStockQuantity()).sum();
        double turnover = currentStock == 0 ? 0 : (double) unitsSoldYtd / (double) Math.max(1, currentStock / 2.0);
        turnover = Math.round(turnover * 10.0) / 10.0;

        List<BiSupplyChainStatDTO> supplyChainStats = List.of(
            new BiSupplyChainStatDTO("Otif Rate", String.format("%.1f%%", otif), "On-time In-full"),
            new BiSupplyChainStatDTO("Lead Time", String.format("%.1f Days", leadTime != null ? leadTime : 0.0), "Avg. Fulfillment"),
            new BiSupplyChainStatDTO("Stock Turnover", turnover + "x", "Annualized"),
            new BiSupplyChainStatDTO("Supplier Risk", supplierMetricsRepository.avgRating() > 3.5 ? "Low" : "Elevated", "Weighted Average")
        );

        // 3. Financial Trend Data (Revenue vs Profit)
        List<Map<String, Object>> trendRaw = (List<Map<String, Object>>) summary("THIS_YEAR").getTrend();
        List<com.phegondev.InventoryManagementSystem.dto.ChartSeriesGroupDTO> financialTrendData = new ArrayList<>();
        
        List<com.phegondev.InventoryManagementSystem.dto.ChartPointDTO> revenueSeries = new ArrayList<>();
        List<com.phegondev.InventoryManagementSystem.dto.ChartPointDTO> profitSeries = new ArrayList<>();

        if (trendRaw != null) {
            for (Map<String, Object> m : trendRaw) {
                String name = (String) m.get("name");
                BigDecimal sales = (BigDecimal) m.get("sales");
                // profit = sales * current calculated avg margin for real-time consistency
                BigDecimal profit = sales.multiply(avgMargin); 
                revenueSeries.add(new com.phegondev.InventoryManagementSystem.dto.ChartPointDTO(name, sales));
                profitSeries.add(new com.phegondev.InventoryManagementSystem.dto.ChartPointDTO(name, profit));
            }
        }
        
        financialTrendData.add(new com.phegondev.InventoryManagementSystem.dto.ChartSeriesGroupDTO("Revenue", revenueSeries));
        financialTrendData.add(new com.phegondev.InventoryManagementSystem.dto.ChartSeriesGroupDTO("Profit", profitSeries));

        // 4. Inventory Mix Data (Category split)
        List<com.phegondev.InventoryManagementSystem.dto.ChartPointDTO> inventoryMixData = new ArrayList<>();
        List<Map<String, Object>> catRaw = (List<Map<String, Object>>) summary("THIS_YEAR").getAnalyticsCategories();
        if (catRaw != null) {
            for (Map<String, Object> m : catRaw) {
                inventoryMixData.add(new com.phegondev.InventoryManagementSystem.dto.ChartPointDTO((String) m.get("category"), (BigDecimal) m.get("revenue")));
            }
        }

        // 5. Ledger Data (Recent Transactions)
        Response dashRes = dashboardService.getSummary();
        List<TransactionDTO> ledgerData = dashRes.getDashboardSummary().getRecentTransactions();

        BiAnalyticsDTO biData = BiAnalyticsDTO.builder()
            .financialStats(financialStats)
            .supplyChainStats(supplyChainStats)
            .financialTrendData(financialTrendData)
            .inventoryMixData(inventoryMixData)
            .ledgerData(ledgerData)
            .build();

        return Response.builder()
            .status(200)
            .message("success")
            .biAnalytics(biData)
            .build();
    }

    private String formatMoney(BigDecimal val) {
        if (val == null) val = BigDecimal.ZERO;
        return "₹" + String.format("%,.0f", val);
    }

    private String calculateTrend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return "N/A";
        BigDecimal diff = current.subtract(previous);
        double pct = diff.multiply(BigDecimal.valueOf(100)).divide(previous, 1, java.math.RoundingMode.HALF_UP).doubleValue();
        return (pct >= 0 ? "+" : "") + pct + "%";
    }

    private String trendClass(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return "trend-neutral";
        return current.compareTo(previous) >= 0 ? "trend-up" : "trend-down";
    }

    private String trendClassInv(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return "trend-neutral";
        // For COGS, higher is technically "down" (bad)
        return current.compareTo(previous) <= 0 ? "trend-up" : "trend-down";
    }
}
