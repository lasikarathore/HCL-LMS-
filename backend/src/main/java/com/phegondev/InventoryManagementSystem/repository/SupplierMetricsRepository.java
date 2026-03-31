package com.phegondev.InventoryManagementSystem.repository;

import com.phegondev.InventoryManagementSystem.entity.SupplierMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SupplierMetricsRepository extends JpaRepository<SupplierMetrics, Long> {

    Optional<SupplierMetrics> findBySupplier_Id(Long supplierId);

    @Query("SELECT COALESCE(AVG(sm.starRating), 0) FROM SupplierMetrics sm")
    double avgRating();

    @Query("SELECT COALESCE(AVG(sm.onTimeDeliveryPercent), 0) FROM SupplierMetrics sm")
    double avgOnTime();

    @Query("SELECT COALESCE(SUM(sm.totalPurchaseValue), 0) FROM SupplierMetrics sm")
    java.math.BigDecimal sumTotalPurchaseValue();

    @Query("SELECT sm FROM SupplierMetrics sm ORDER BY sm.onTimeDeliveryPercent DESC, sm.starRating DESC")
    List<SupplierMetrics> topByPerformance(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT sm FROM SupplierMetrics sm ORDER BY sm.onTimeDeliveryPercent ASC, sm.starRating ASC")
    List<SupplierMetrics> bottomByPerformance(org.springframework.data.domain.Pageable pageable);
}

