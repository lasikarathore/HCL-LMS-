package com.phegondev.InventoryManagementSystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionAnalyticsDTO {

    /** Count of transactions per {@link com.phegondev.InventoryManagementSystem.enums.TransactionType} */
    private List<ChartPointDTO> countByType;

    /** Sum of {@code totalPrice} per transaction type */
    private List<ChartPointDTO> amountByType;

    /** Total amount per day for the requested calendar month (when {@code month} + {@code year} query params sent) */
    private List<ChartPointDTO> dailyAmountsInMonth;

    /** Up to 12 recent calendar months: grouped amounts per transaction type (for grouped bar chart) */
    private List<ChartSeriesGroupDTO> monthlyAmountByType;

    /** Up to 12 recent months: total volume per month (for line chart) */
    private List<ChartPointDTO> monthlyTotalVolume;

    /** Transaction hub: 7-day insight row (sparklines + trends) */
    private List<DashboardSparklineCardDTO> insightCards;

    private List<ChartPointDTO> sevenDayVolumeBars;
    private List<ChartPointDTO> sevenDayVolumeLine;

    /** Revenue concentration — top SKUs last 30 days */
    private List<HorizontalMetricDTO> topProductVolumeShare;

    private List<ChartPointDTO> volumeDonutByTypeLast30Days;

    /** Wallet-style “order book” rows */
    private List<TopProductRowDTO> topProductLeaders;
}
