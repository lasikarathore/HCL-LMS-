package com.phegondev.InventoryManagementSystem.service.impl;

import com.phegondev.InventoryManagementSystem.dto.StockAlertDTO;
import com.phegondev.InventoryManagementSystem.entity.Product;
import com.phegondev.InventoryManagementSystem.entity.StockAlert;
import com.phegondev.InventoryManagementSystem.enums.StockAlertStatus;
import com.phegondev.InventoryManagementSystem.repository.ProductRepository;
import com.phegondev.InventoryManagementSystem.repository.StockAlertRepository;
import com.phegondev.InventoryManagementSystem.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class StockAlertServiceImpl implements StockAlertService {

    private final StockAlertRepository stockAlertRepository;
    private final ProductRepository productRepository;
    private final AtomicBoolean initialSyncDone = new AtomicBoolean(false);

    private StockAlertStatus determineStatus(int stockQuantity) {
        if (stockQuantity < StockAlertStatus.CRITICAL.getThreshold()) return StockAlertStatus.CRITICAL;
        if (stockQuantity < StockAlertStatus.LOW.getThreshold()) return StockAlertStatus.LOW;
        if (stockQuantity < StockAlertStatus.WATCH.getThreshold()) return StockAlertStatus.WATCH;
        return null;
    }

    private int severityRank(StockAlertStatus status) {
        return switch (status) {
            case CRITICAL -> 0;
            case LOW -> 1;
            case WATCH -> 2;
        };
    }

    @Override
    @Transactional
    public void reconcileAfterStockChange(Product product) {
        if (product == null || product.getId() == null) return;

        int stockQuantity = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        StockAlertStatus newStatus = determineStatus(stockQuantity);

        List<StockAlert> existingUnresolved =
                stockAlertRepository.findByProduct_IdAndResolvedFalseOrderByCreatedAtDesc(product.getId());

        if (newStatus == null) {
            for (StockAlert existing : existingUnresolved) {
                existing.setResolved(true);
                existing.setResolvedAt(LocalDateTime.now());
                existing.setUpdatedAt(LocalDateTime.now());
                stockAlertRepository.save(existing);
            }
            return;
        }

        if (existingUnresolved.isEmpty()) {
            StockAlert alert = StockAlert.builder()
                    .product(product)
                    .status(newStatus)
                    .threshold(newStatus.getThreshold())
                    .stockQuantityAtAlert(stockQuantity)
                    .resolved(false)
                    .build();
            stockAlertRepository.save(alert);
            return;
        }

        // Keep ONE active alert row per product.
        StockAlert primary = existingUnresolved.get(0);
        primary.setStatus(newStatus);
        primary.setThreshold(newStatus.getThreshold());
        primary.setStockQuantityAtAlert(stockQuantity);
        primary.setResolved(false);
        primary.setResolvedAt(null);
        primary.setUpdatedAt(LocalDateTime.now());
        stockAlertRepository.save(primary);

        for (int i = 1; i < existingUnresolved.size(); i++) {
            StockAlert dup = existingUnresolved.get(i);
            dup.setResolved(true);
            dup.setResolvedAt(LocalDateTime.now());
            dup.setUpdatedAt(LocalDateTime.now());
            stockAlertRepository.save(dup);
        }
    }

    private void ensureInitialSync() {
        if (!initialSyncDone.compareAndSet(false, true)) return;

        int watchThreshold = StockAlertStatus.WATCH.getThreshold();
        List<Product> lowStockProducts =
                productRepository.findByStockQuantityLessThanOrderByStockQuantityAsc(watchThreshold);
        for (Product p : lowStockProducts) {
            reconcileAfterStockChange(p);
        }
    }

    @Override
    @Transactional
    public long countActiveAlerts() {
        ensureInitialSync();
        return stockAlertRepository.countDistinctActiveProducts();
    }

    @Override
    @Transactional
    public List<StockAlertDTO> getActiveAlerts() {
        ensureInitialSync();
        List<StockAlert> unresolved = stockAlertRepository.findByResolvedFalseOrderByCreatedAtDesc();

        // Deduplicate by product.
        Map<Long, StockAlert> bestByProductId = new HashMap<>();
        for (StockAlert a : unresolved) {
            if (a.getProduct() == null || a.getProduct().getId() == null) continue;
            Long pid = a.getProduct().getId();
            StockAlert cur = bestByProductId.get(pid);
            if (cur == null) {
                bestByProductId.put(pid, a);
                continue;
            }
            int ar = a.getStatus() == null ? 9 : severityRank(a.getStatus());
            int cr = cur.getStatus() == null ? 9 : severityRank(cur.getStatus());
            if (ar < cr) {
                bestByProductId.put(pid, a);
                continue;
            }
            if (ar == cr) {
                Integer as = a.getStockQuantityAtAlert();
                Integer cs = cur.getStockQuantityAtAlert();
                if (as != null && cs != null && as < cs) {
                    bestByProductId.put(pid, a);
                }
            }
        }

        List<StockAlert> alerts = new java.util.ArrayList<>(bestByProductId.values().stream().toList());
        alerts.sort(Comparator.comparing((StockAlert a) -> a.getStatus() == null ? 9 : severityRank(a.getStatus()))
                .thenComparing(StockAlert::getStockQuantityAtAlert, Comparator.nullsLast(Integer::compareTo)));

        return alerts.stream().map(a -> StockAlertDTO.builder()
                .id(a.getId())
                .productId(a.getProduct() != null ? a.getProduct().getId() : null)
                .productName(a.getProduct() != null ? a.getProduct().getName() : "Unknown Product")
                .productSku(a.getProduct() != null ? a.getProduct().getSku() : "N/A")
                .currentStock(a.getStockQuantityAtAlert())
                .threshold(a.getThreshold())
                .status(a.getStatus() == null ? "N/A" : a.getStatus().getLabel())
                .createdAt(a.getCreatedAt())
                .resolvedAt(a.getResolvedAt())
                .build()).toList();
    }
}

