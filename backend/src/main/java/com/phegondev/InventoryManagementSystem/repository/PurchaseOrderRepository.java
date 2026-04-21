package com.phegondev.InventoryManagementSystem.repository;

import com.phegondev.InventoryManagementSystem.entity.PurchaseOrder;
import com.phegondev.InventoryManagementSystem.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    long countByStatus(PurchaseOrderStatus status);
    boolean existsByPoNumber(String poNumber);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400) FROM purchase_orders WHERE status = 'RECEIVED'", nativeQuery = true)
    Double avgLeadTimeDays();
    @Query(value = "SELECT poi.unit_price FROM purchase_order_items poi JOIN purchase_orders po ON poi.purchase_order_id = po.id WHERE poi.product_id = :productId AND po.status = 'RECEIVED' ORDER BY po.created_at DESC LIMIT 1", nativeQuery = true)
    BigDecimal findLatestPurchasePrice(@Param("productId") Long productId);
}

