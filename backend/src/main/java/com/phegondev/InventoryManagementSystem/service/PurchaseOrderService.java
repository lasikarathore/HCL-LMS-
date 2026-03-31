package com.phegondev.InventoryManagementSystem.service;

import com.phegondev.InventoryManagementSystem.dto.PurchaseOrderCreateRequest;
import com.phegondev.InventoryManagementSystem.dto.PurchaseOrderDTO;
import com.phegondev.InventoryManagementSystem.dto.Response;

public interface PurchaseOrderService {
    Response createPurchaseOrder(PurchaseOrderCreateRequest req);

    Response listPurchaseOrders(String status);

    Response getPurchaseOrder(Long id);

    Response approvePurchaseOrder(Long id);

    Response receivePurchaseOrder(Long id);

    Response summary();
}

