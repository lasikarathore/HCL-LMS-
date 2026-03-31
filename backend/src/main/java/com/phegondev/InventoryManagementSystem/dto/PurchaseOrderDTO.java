package com.phegondev.InventoryManagementSystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseOrderDTO {
    private Long id;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private LocalDate requiredByDate;
    private LocalDate expectedDeliveryDate;
    private String status;
    private BigDecimal totalValue;
    private String priority;
    private String notes;
    private LocalDateTime createdAt;
    private List<PurchaseOrderItemDTO> items;
}

