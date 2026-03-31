package com.phegondev.InventoryManagementSystem.controller;

import com.phegondev.InventoryManagementSystem.dto.PurchaseOrderCreateRequest;
import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<Response> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(purchaseOrderService.listPurchaseOrders(status));
    }

    @GetMapping("/summary")
    public ResponseEntity<Response> summary() {
        return ResponseEntity.ok(purchaseOrderService.summary());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> get(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrder(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> create(@RequestBody @Valid PurchaseOrderCreateRequest req) {
        return ResponseEntity.ok(purchaseOrderService.createPurchaseOrder(req));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> approve(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.approvePurchaseOrder(id));
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> receive(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.receivePurchaseOrder(id));
    }
}

