package com.phegondev.InventoryManagementSystem.controller;

import com.phegondev.InventoryManagementSystem.dto.Response;
import com.phegondev.InventoryManagementSystem.dto.StockAlertDTO;
import com.phegondev.InventoryManagementSystem.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

        private final StockAlertService stockAlertService;

        @GetMapping("/count")
        public ResponseEntity<Response> count() {
                long count = stockAlertService.countActiveAlerts();
                return ResponseEntity.ok(Response.builder()
                                .status(200)
                                .message("success")
                                .alertCount((int) Math.min(Integer.MAX_VALUE, count))
                                .build());
        }

        @GetMapping
        public ResponseEntity<Response> list() {
                List<StockAlertDTO> alerts = stockAlertService.getActiveAlerts();
                return ResponseEntity.ok(Response.builder()
                                .status(200)
                                .message("success")
                                .alerts(alerts)
                                .build());
        }
}
