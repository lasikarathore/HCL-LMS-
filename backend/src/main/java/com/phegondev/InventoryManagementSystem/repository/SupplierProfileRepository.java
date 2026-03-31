package com.phegondev.InventoryManagementSystem.repository;

import com.phegondev.InventoryManagementSystem.entity.SupplierProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierProfileRepository extends JpaRepository<SupplierProfile, Long> {
    Optional<SupplierProfile> findBySupplier_Id(Long supplierId);
}

