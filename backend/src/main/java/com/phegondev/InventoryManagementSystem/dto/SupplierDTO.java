package com.phegondev.InventoryManagementSystem.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplierDTO {

    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String address;

    private String status;

    // Enhanced supplier management fields (stored in SupplierProfile)
    private String email;
    private String categorySpecialisation;
    private String paymentTerms;
    private Double starRating;
    private Integer onTimeDeliveryPercent;
    private Boolean active;

    // Analytics/computed fields (optional)
    private Long itemsSupplied;
    private java.math.BigDecimal totalPurchaseValue;
}
