package com.phegondev.InventoryManagementSystem.service.impl;

import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.dto.SupplierManagementDTO;
import com.phegondev.InventoryManagementSystem.entity.Supplier;
import com.phegondev.InventoryManagementSystem.entity.SupplierMetrics;
import com.phegondev.InventoryManagementSystem.exceptions.NotFoundException;
import com.phegondev.InventoryManagementSystem.repository.SupplierMetricsRepository;
import com.phegondev.InventoryManagementSystem.repository.SupplierRepository;
import com.phegondev.InventoryManagementSystem.service.SupplierManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierManagementServiceImpl implements SupplierManagementService {

    private final SupplierRepository supplierRepository;
    private final SupplierMetricsRepository supplierMetricsRepository;

    private SupplierManagementDTO toDto(Supplier s, SupplierMetrics m) {
        return SupplierManagementDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .address(s.getAddress())
                .email(m != null ? m.getEmail() : null)
                .starRating(m != null ? m.getStarRating() : 0.0)
                .onTimeDeliveryPercent(m != null ? m.getOnTimeDeliveryPercent() : 0.0)
                .itemsSupplied(0) // No line-item history table; keep 0 for now
                .active(m == null || m.isActive())
                .totalPurchaseValue(m != null ? m.getTotalPurchaseValue() : BigDecimal.ZERO)
                .categorySpecialisation(m != null ? m.getCategorySpecialisation() : null)
                .paymentTerms(m != null ? m.getPaymentTerms() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response list() {
        List<Supplier> suppliers = supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<SupplierManagementDTO> out = suppliers.stream().map(s -> {
            SupplierMetrics m = supplierMetricsRepository.findBySupplier_Id(s.getId()).orElse(null);
            return toDto(s, m);
        }).toList();

        return Response.builder()
                .status(200)
                .message("success")
                .supplierManagement(out)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response summary() {
        long total = supplierRepository.count();
        double avgRating = supplierMetricsRepository.avgRating();
        double avgOnTime = supplierMetricsRepository.avgOnTime();
        BigDecimal totalPurchaseValue = supplierMetricsRepository.sumTotalPurchaseValue();

        SupplierMetrics top = supplierMetricsRepository.topByPerformance(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        SupplierMetrics bottom = supplierMetricsRepository.bottomByPerformance(PageRequest.of(0, 1)).stream().findFirst().orElse(null);

        SupplierManagementDTO topDto = null;
        SupplierManagementDTO bottomDto = null;
        if (top != null && top.getSupplier() != null) {
            topDto = toDto(top.getSupplier(), top);
        }
        if (bottom != null && bottom.getSupplier() != null) {
            bottomDto = toDto(bottom.getSupplier(), bottom);
        }

        return Response.builder()
                .status(200)
                .message("success")
                .supplierTotal(total)
                .supplierAvgRating(avgRating)
                .supplierAvgOnTime(avgOnTime)
                .supplierTotalPurchaseValue(totalPurchaseValue)
                .supplierTopPerformer(topDto)
                .supplierNeedsAttention(bottomDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response get(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier Not Found"));
        SupplierMetrics m = supplierMetricsRepository.findBySupplier_Id(supplierId).orElse(null);
        return Response.builder()
                .status(200)
                .message("success")
                .supplierTopPerformer(toDto(supplier, m)) // reuse field to avoid adding new response field
                .build();
    }

    @Override
    @Transactional
    public Response upsert(SupplierManagementDTO dto) {
        Supplier supplier;
        if (dto.getId() != null) {
            supplier = supplierRepository.findById(dto.getId())
                    .orElseThrow(() -> new NotFoundException("Supplier Not Found"));
            if (dto.getName() != null && !dto.getName().isBlank()) {
                supplier.setName(dto.getName());
            }
            if (dto.getAddress() != null) {
                supplier.setAddress(dto.getAddress());
            }
            supplierRepository.save(supplier);
        } else {
            supplier = Supplier.builder()
                    .name(dto.getName())
                    .address(dto.getAddress())
                    .build();
            supplier = supplierRepository.save(supplier);
        }

        SupplierMetrics metrics = supplierMetricsRepository.findBySupplier_Id(supplier.getId()).orElse(null);
        if (metrics == null) {
            metrics = SupplierMetrics.builder()
                    .supplier(supplier)
                    .build();
        }
        metrics.setEmail(dto.getEmail());
        metrics.setCategorySpecialisation(dto.getCategorySpecialisation());
        metrics.setPaymentTerms(dto.getPaymentTerms());
        metrics.setStarRating(dto.getStarRating());
        metrics.setOnTimeDeliveryPercent(dto.getOnTimeDeliveryPercent());
        metrics.setActive(dto.getActive() == null ? true : dto.getActive());
        metrics.setTotalPurchaseValue(dto.getTotalPurchaseValue() == null ? BigDecimal.ZERO : dto.getTotalPurchaseValue());
        metrics.setUpdatedAt(LocalDateTime.now());
        supplierMetricsRepository.save(metrics);

        return Response.builder()
                .status(200)
                .message("Supplier saved")
                .supplierId(supplier.getId())
                .build();
    }
}

