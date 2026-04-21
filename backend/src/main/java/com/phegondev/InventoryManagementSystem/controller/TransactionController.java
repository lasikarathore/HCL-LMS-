package com.phegondev.InventoryManagementSystem.controller;

import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.dto.TransactionRequest;
import com.phegondev.InventoryManagementSystem.enums.TransactionStatus;
import com.phegondev.InventoryManagementSystem.enums.TransactionType;
import com.phegondev.InventoryManagementSystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PROCUREMENT_OFFICER', 'WAREHOUSE_MANAGER', 'STAFF', 'ANALYST')")
    public ResponseEntity<Response> getTransactionAnalytics(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(transactionService.getTransactionAnalytics(month, year));
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<Response> restockInventory(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.restockInventory(transactionRequest));
    }
    @PostMapping("/sell")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<Response> sell(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.sell(transactionRequest));
    }
    @PostMapping("/return")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<Response> returnToSupplier(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.returnToSupplier(transactionRequest));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF', 'WAREHOUSE_MANAGER', 'PROCUREMENT_OFFICER', 'ANALYST')")
    public ResponseEntity<Response> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(transactionService.getAllTransactions(page, size, searchText, transactionType, status));
    }

    @GetMapping("/by-month-year")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WAREHOUSE_MANAGER', 'PROCUREMENT_OFFICER', 'ANALYST')")
    public ResponseEntity<Response> getAllTransactionByMonthAndYear(
            @RequestParam int month,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(transactionService.getAllTransactionByMonthAndYear(month, year));
    }

    /**
     * Only numeric segments match — avoids "analytics", "all", etc. binding as {@code id}.
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF', 'WAREHOUSE_MANAGER', 'PROCUREMENT_OFFICER', 'ANALYST')")
    public ResponseEntity<Response> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @PutMapping("/update/{transactionId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> updateTransactionStatus(
            @PathVariable Long transactionId,
            @RequestBody @Valid TransactionStatus status) {
        System.out.println("ID IS: " + transactionId);
        System.out.println("Status IS: " + status);
        return ResponseEntity.ok(transactionService.updateTransactionStatus(transactionId, status));
    }




}
