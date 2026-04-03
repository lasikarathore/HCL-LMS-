package com.phegondev.InventoryManagementSystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierManagementDTO {
    private Long id;
    private String name;
    private String address;
    private String email;
    private Double starRating;
    private Double onTimeDeliveryPercent;
    private Integer itemsSupplied;
    private Boolean active;
    private BigDecimal totalPurchaseValue;
    private String categorySpecialisation;
    private String paymentTerms;
    private String status;
}

