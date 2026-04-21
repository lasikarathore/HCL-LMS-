package com.phegondev.InventoryManagementSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BiAnalyticsDTO {
    
    private List<BiFinancialKpiDTO> financialStats;
    private List<BiSupplyChainStatDTO> supplyChainStats;
    private List<ChartSeriesGroupDTO> financialTrendData;
    private List<ChartPointDTO> inventoryMixData;
    private List<TransactionDTO> ledgerData;
}
