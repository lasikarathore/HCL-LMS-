package com.phegondev.InventoryManagementSystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "supplier_metrics")
public class SupplierMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", unique = true, nullable = false)
    private Supplier supplier;

    private String email;

    private String categorySpecialisation;

    private String paymentTerms; // Net 30 / Net 15 / Net 60 / Immediate

    /** 0..5 */
    private Double starRating;

    /** 0..100 */
    private Double onTimeDeliveryPercent;

    /** 0..100 */
    private Double qualityScorePercent;

    /** 0..100 (higher = faster) */
    private Double responseTimeScore;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private BigDecimal totalPurchaseValue = BigDecimal.ZERO;

    private LocalDateTime updatedAt;

    private final LocalDateTime createdAt = LocalDateTime.now();
}

