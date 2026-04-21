package com.phegondev.InventoryManagementSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BiSupplyChainStatDTO {
    private String label;
    private String value;
    private String sub; // descriptive subtitle
}
