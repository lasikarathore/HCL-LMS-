package com.phegondev.InventoryManagementSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BiFinancialKpiDTO {
    private String label;
    private String value;
    private String trend;
    private String trendClass; // e.g., 'trend-up', 'trend-down'
}
