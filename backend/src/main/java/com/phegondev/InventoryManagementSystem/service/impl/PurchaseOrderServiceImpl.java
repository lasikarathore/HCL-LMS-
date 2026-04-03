package com.phegondev.InventoryManagementSystem.service.impl;

import com.phegondev.InventoryManagementSystem.dto.PurchaseOrderCreateRequest;
import com.phegondev.InventoryManagementSystem.dto.PurchaseOrderDTO;
import com.phegondev.InventoryManagementSystem.dto.PurchaseOrderItemDTO;
import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.entity.Product;
import com.phegondev.InventoryManagementSystem.entity.PurchaseOrder;
import com.phegondev.InventoryManagementSystem.entity.PurchaseOrderItem;
import com.phegondev.InventoryManagementSystem.entity.Supplier;
import com.phegondev.InventoryManagementSystem.enums.PurchaseOrderStatus;
import com.phegondev.InventoryManagementSystem.exceptions.NotFoundException;
import com.phegondev.InventoryManagementSystem.repository.ProductRepository;
import com.phegondev.InventoryManagementSystem.repository.PurchaseOrderRepository;
import com.phegondev.InventoryManagementSystem.repository.SupplierRepository;
import com.phegondev.InventoryManagementSystem.repository.TransactionRepository;
import com.phegondev.InventoryManagementSystem.service.PurchaseOrderService;
import com.phegondev.InventoryManagementSystem.service.StockAlertService;
import com.phegondev.InventoryManagementSystem.entity.*;
import com.phegondev.InventoryManagementSystem.enums.*;
import com.phegondev.InventoryManagementSystem.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final com.phegondev.InventoryManagementSystem.repository.SupplierProfileRepository supplierProfileRepository;
    private final com.phegondev.InventoryManagementSystem.service.UserService userService;
    private final StockAlertService stockAlertService;

    private static String formatPoNumber(long id, int year) {
        return String.format("PO-%04d-%03d", year, id);
    }

    private String nextPoNumber() {
        int year = java.time.LocalDate.now().getYear();
        long n = purchaseOrderRepository.count() + 1;
        String candidate = formatPoNumber(n, year);
        while (purchaseOrderRepository.existsByPoNumber(candidate)) {
            n++;
            candidate = formatPoNumber(n, year);
        }
        return candidate;
    }

    private static PurchaseOrderDTO toDto(PurchaseOrder po) {
        List<PurchaseOrderItemDTO> items = (po.getItems() == null ? List.<PurchaseOrderItem>of() : po.getItems())
                .stream()
                .map(i -> PurchaseOrderItemDTO.builder()
                        .id(i.getId())
                        .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                        .productName(i.getProduct() != null ? i.getProduct().getName() : null)
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .build())
                .toList();

        return PurchaseOrderDTO.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : null)
                .requiredByDate(po.getRequiredByDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .status(po.getStatus() != null ? po.getStatus().name() : null)
                .totalValue(po.getTotalValue())
                .priority(po.getPriority())
                .notes(po.getNotes())
                .createdAt(po.getCreatedAt())
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public Response createPurchaseOrder(PurchaseOrderCreateRequest req) {
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new NotFoundException("Supplier Not Found"));

        // Check if supplier is active via profile
        supplierProfileRepository.findBySupplier_Id(supplier.getId()).ifPresent(profile -> {
            if (profile.getActive() != null && !profile.getActive()) {
                throw new com.phegondev.InventoryManagementSystem.exceptions.ConflictException(
                        "Supplier '" + supplier.getName() + "' is currently INACTIVE and cannot be used for new orders.");
            }
        });

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new NotFoundException("Product Not Found"));

        BigDecimal qty = BigDecimal.valueOf(req.getQuantity());
        BigDecimal total = req.getUnitPrice().multiply(qty);

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(nextPoNumber()) // Set before first insert (legacy DB has NOT NULL on order_number).
                .supplier(supplier)
                .createdBy(userService.getCurrentLoggedInUser())
                .requiredByDate(req.getRequiredByDate())
                .expectedDeliveryDate(req.getExpectedDeliveryDate())
                .priority(req.getPriority())
                .notes(req.getNotes())
                .status(PurchaseOrderStatus.PENDING)
                .totalValue(total)
                .build();

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .product(product)
                .quantity(req.getQuantity())
                .unitPrice(req.getUnitPrice())
                .lineTotal(total)
                .build();

        po.getItems().add(item);

        PurchaseOrder saved = purchaseOrderRepository.save(po);

        return Response.builder()
                .status(200)
                .message("Purchase order created")
                .purchaseOrder(toDto(saved))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response listPurchaseOrders(String status) {
        List<PurchaseOrder> rows = purchaseOrderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<PurchaseOrderDTO> dtos = rows.stream()
                .filter(po -> {
                    if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) return true;
                    return po.getStatus() != null && po.getStatus().name().equalsIgnoreCase(status);
                })
                .map(PurchaseOrderServiceImpl::toDto)
                .toList();

        return Response.builder()
                .status(200)
                .message("success")
                .purchaseOrders(dtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getPurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
        return Response.builder()
                .status(200)
                .message("success")
                .purchaseOrder(toDto(po))
                .build();
    }

    @Override
    @Transactional
    public Response approvePurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setUpdatedAt(LocalDateTime.now());
        purchaseOrderRepository.save(po);
        return Response.builder()
                .status(200)
                .message("Approved")
                .purchaseOrder(toDto(po))
                .build();
    }

    @Override
    @Transactional
    public Response receivePurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));

        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new RuntimeException("Purchase order already received");
        }
        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new RuntimeException("Only APPROVED purchase orders can be received. Current status: " + po.getStatus());
        }

        po.setStatus(PurchaseOrderStatus.RECEIVED);
        po.setUpdatedAt(LocalDateTime.now());

        // Update Stock and Log Transactions
        for (PurchaseOrderItem item : po.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);

                // Create Transaction record
                Transaction transaction = Transaction.builder()
                        .product(product)
                        .supplier(po.getSupplier())
                        .user(userService.getCurrentLoggedInUser())
                        .transactionType(TransactionType.PURCHASE)
                        .status(null) // Keep null to avoid existing DB status check constraint violation
                        .totalProducts(item.getQuantity())
                        .totalPrice(item.getLineTotal())
                        .description("Stock received from PO: " + po.getPoNumber())
                        .build();
                transactionRepository.save(transaction);
                stockAlertService.reconcileAfterStockChange(product);
            }
        }

        purchaseOrderRepository.save(po);
        return Response.builder()
                .status(200)
                .message("Marked as received, stock updated and transaction logged")
                .purchaseOrder(toDto(po))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response summary() {
        long total = purchaseOrderRepository.count();
        long pending = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.PENDING);
        long approved = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.APPROVED);
        long received = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.RECEIVED);
        return Response.builder()
                .status(200)
                .message("success")
                .poTotal(total)
                .poPending(pending)
                .poApproved(approved)
                .poReceived(received)
                .build();
    }
}

