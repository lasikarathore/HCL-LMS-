package com.phegondev.InventoryManagementSystem.service;

import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.dto.SupplierManagementDTO;

public interface SupplierManagementService {
    Response summary();
    Response list();
    Response upsert(SupplierManagementDTO dto);
    Response get(Long supplierId);
}

