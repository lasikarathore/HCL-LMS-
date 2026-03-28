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
public class DashboardSparklineCardDTO {

    private String title;
    private String badgeText;
    /** Currency amount or whole number depending on card */
    private BigDecimal primaryValue;
    /** When true, format primary as currency in UI */
    private boolean currency;
    /** Percent change vs prior 7-day window (can be negative) */
    private BigDecimal trendPercentVsPriorWeek;
    private boolean trendPositive;
    /** Seven points (one per day), aligned with current 7-day window */
    private List<BigDecimal> sparkline;
}
