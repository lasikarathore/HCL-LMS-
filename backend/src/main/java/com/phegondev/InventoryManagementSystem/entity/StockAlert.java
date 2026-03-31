package com.phegondev.InventoryManagementSystem.entity;

import com.phegondev.InventoryManagementSystem.enums.StockAlertStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "stock_alerts")
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    private StockAlertStatus status;

    private Integer threshold;

    private Integer stockQuantityAtAlert;

    @Builder.Default
    private boolean resolved = false;

    private LocalDateTime resolvedAt;

    private LocalDateTime updatedAt;

    private final LocalDateTime createdAt = LocalDateTime.now();
}

