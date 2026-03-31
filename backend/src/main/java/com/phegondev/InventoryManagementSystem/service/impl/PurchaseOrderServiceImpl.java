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
import com.phegondev.InventoryManagementSystem.service.PurchaseOrderService;
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
    private final com.phegondev.InventoryManagementSystem.service.UserService userService;

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
        po.setStatus(PurchaseOrderStatus.RECEIVED);
        po.setUpdatedAt(LocalDateTime.now());
        purchaseOrderRepository.save(po);
        return Response.builder()
                .status(200)
                .message("Marked as received")
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

