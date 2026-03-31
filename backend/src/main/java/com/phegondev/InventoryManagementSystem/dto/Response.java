package com.phegondev.InventoryManagementSystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.phegondev.InventoryManagementSystem.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {
    // generic
    private int status;
    private String message;
    // for login
    private String token;
    private UserRole role;
    private String expirationTime;

    // for pagination
    private Integer totalPages;
    private Long totalElements;

    // data output optional
    private UserDTO user;
    private List<UserDTO> users;

    private SupplierDTO supplier;
    private List<SupplierDTO> suppliers;

    private CategoryDTO category;
    private List<CategoryDTO> categories;

    private ProductDTO product;
    private List<ProductDTO> products;

    private TransactionDTO transaction;
    private List<TransactionDTO> transactions;

    private DashboardSummaryDTO dashboardSummary;

    private TransactionAnalyticsDTO transactionAnalytics;

    // Stock alerts
    private Integer alertCount;
    private List<StockAlertDTO> alerts;

    // Purchase Orders
    private PurchaseOrderDTO purchaseOrder;
    private List<PurchaseOrderDTO> purchaseOrders;
    private Long poTotal;
    private Long poPending;
    private Long poApproved;
    private Long poReceived;

    // Supplier management
    private List<SupplierManagementDTO> supplierManagement;
    private SupplierManagementDTO supplierTopPerformer;
    private SupplierManagementDTO supplierNeedsAttention;
    private Long supplierTotal;
    private Double supplierAvgRating;
    private Double supplierAvgOnTime;
    private java.math.BigDecimal supplierTotalPurchaseValue;

    // Analytics (simple payloads for new UI)
    private java.math.BigDecimal revenue;
    private java.math.BigDecimal grossProfit;
    private Integer grossMargin;
    private java.math.BigDecimal inventoryValue;
    private Double turnoverRate;
    private Double industryAvg;
    private List<java.util.Map<String, Object>> trend;
    private List<java.util.Map<String, Object>> analyticsCategories;
    private List<java.util.Map<String, Object>> rows;
    private List<java.util.Map<String, Object>> analyticsSuppliers;
    private String insight;

    // Identifiers for create/update operations
    private Long supplierId;

    private final LocalDateTime timestamp = LocalDateTime.now();

}
