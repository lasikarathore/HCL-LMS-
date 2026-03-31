package com.phegondev.InventoryManagementSystem.repository;

import com.phegondev.InventoryManagementSystem.entity.PurchaseOrder;
import com.phegondev.InventoryManagementSystem.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    long countByStatus(PurchaseOrderStatus status);
    boolean existsByPoNumber(String poNumber);
}

