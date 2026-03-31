package com.phegondev.InventoryManagementSystem.service;

import com.phegondev.InventoryManagementSystem.dto.StockAlertDTO;
import com.phegondev.InventoryManagementSystem.entity.Product;

import java.util.List;

public interface StockAlertService {
    void reconcileAfterStockChange(Product product);

    long countActiveAlerts();

    List<StockAlertDTO> getActiveAlerts();
}

