package com.phegondev.InventoryManagementSystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PurchaseOrderCreateRequest {
    @NotNull
    @Positive
    private Long supplierId;

    private LocalDate requiredByDate;

    private LocalDate expectedDeliveryDate;

    @NotNull
    private String priority; // Normal / High / Urgent

    private String notes;

    @NotNull
    @Positive
    private Long productId;

    @NotNull
    @Positive
    private Integer quantity;

    @NotNull
    private BigDecimal unitPrice;
}

