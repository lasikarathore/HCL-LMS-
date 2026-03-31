package com.phegondev.InventoryManagementSystem.repository;

import com.phegondev.InventoryManagementSystem.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    @Query("SELECT COUNT(DISTINCT sa.product.id) FROM StockAlert sa WHERE sa.resolved = false")
    long countDistinctActiveProducts();

    List<StockAlert> findByResolvedFalseOrderByCreatedAtDesc();

    List<StockAlert> findByProduct_IdAndResolvedFalseOrderByCreatedAtDesc(Long productId);

    Optional<StockAlert> findTopByProduct_IdAndResolvedFalseOrderByCreatedAtDesc(Long productId);
}

