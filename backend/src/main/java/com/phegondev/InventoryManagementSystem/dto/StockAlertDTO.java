package com.phegondev.InventoryManagementSystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockAlertDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer currentStock;
    private Integer threshold;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}

