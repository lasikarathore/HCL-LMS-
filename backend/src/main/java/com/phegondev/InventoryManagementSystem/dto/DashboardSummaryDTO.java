package com.phegondev.InventoryManagementSystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummaryDTO {

    private long totalProducts;
    private long totalCategories;
    private long totalSuppliers;
    private BigDecimal inventoryValue;
    private long lowStockProductCount;
    private int lowStockThreshold;
    private BigDecimal salesRevenueThisMonth;
    private long purchaseTransactionsThisMonth;
    private long saleTransactionsThisMonth;

    private BigDecimal mySalesToday;
    private long myTransactionsCount;

    private List<ProductDTO> lowStockProducts;
    private List<TransactionDTO> recentTransactions;

    /** Reference UI: pastel summary row with sparklines (Wallet / Analytics style) */
    private List<DashboardSparklineCardDTO> insightCards;

    /** Last 7 days total volume per day — main vertical bar chart */
    private List<ChartPointDTO> sevenDayVolumeBars;

    /** Same series as line chart (market overview style) */
    private List<ChartPointDTO> sevenDayVolumeLine;

    /** Category share of inventory value — right-rail horizontal bars */
    private List<HorizontalMetricDTO> categoryInventoryShare;

    /** Donut: transaction-type mix for last 30 days */
    private List<ChartPointDTO> activityDonutLast30Days;
}
