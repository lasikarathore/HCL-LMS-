package com.phegondev.InventoryManagementSystem.service;

import com.phegondev.InventoryManagementSystem.dto.Response;

public interface AnalyticsService {
    Response summary(String range);
    Response getBiSummary();
}

