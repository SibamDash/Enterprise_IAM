package com.enterprise.iam.controller;

import com.enterprise.iam.dto.DashboardMetricsDto;
import com.enterprise.iam.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('ADMIN_READ') or hasAuthority('DASHBOARD_READ')")
    public DashboardMetricsDto getMetrics() {
        return dashboardService.getMetrics();
    }
}
