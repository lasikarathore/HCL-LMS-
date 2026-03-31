package com.phegondev.InventoryManagementSystem.enums;

public enum StockAlertStatus {
    CRITICAL(5, "Critical"),
    LOW(10, "Low"),
    WATCH(20, "Watch");

    private final int threshold;
    private final String label;

    StockAlertStatus(int threshold, String label) {
        this.threshold = threshold;
        this.label = label;
    }

    public int getThreshold() {
        return threshold;
    }

    public String getLabel() {
        return label;
    }
}

