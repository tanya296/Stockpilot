package com.stockpilot.controller;

import com.stockpilot.dto.DashboardDtos.DashboardSummary;
import com.stockpilot.dto.DashboardDtos.LowStockItem;
import com.stockpilot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return dashboardService.summary();
    }

    @GetMapping("/low-stock")
    public List<LowStockItem> lowStock() {
        return dashboardService.lowStock();
    }
}
