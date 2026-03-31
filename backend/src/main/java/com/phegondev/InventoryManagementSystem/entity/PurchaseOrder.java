package com.phegondev.InventoryManagementSystem.entity;

import com.phegondev.InventoryManagementSystem.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable identifier: PO-2026-001 (mapped to legacy DB column). */
    @Column(name = "order_number", unique = true, nullable = false)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    private LocalDate requiredByDate;

    private LocalDate expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus status;

    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    private String priority; // Normal/High/Urgent (kept simple)

    @Column(length = 2000)
    private String notes;

    private LocalDateTime updatedAt;

    private final LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();
}

