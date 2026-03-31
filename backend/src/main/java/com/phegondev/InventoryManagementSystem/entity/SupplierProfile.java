package com.phegondev.InventoryManagementSystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "supplier_profiles")
public class SupplierProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", unique = true, nullable = false)
    private Supplier supplier;

    private String email;

    private String categorySpecialisation;

    private String paymentTerms; // Net 30 / Net 15 / Net 60 / Immediate

    /** 0.0 - 5.0 */
    private Double starRating;

    /** 0 - 100 */
    private Integer onTimeDeliveryPercent;

    /** Active/Inactive */
    @Builder.Default
    private Boolean active = true;

    /** Optional UX metrics for panels */
    private Integer qualityScorePercent;
    private Integer responseTimeHours;
}

