package com.ticketflow.analytics;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketflow.analytics.dto.DashboardSummaryResponse;
import com.ticketflow.auth.UserPrincipal;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardAnalyticsService dashboardAnalyticsService;

    public DashboardController(DashboardAnalyticsService dashboardAnalyticsService) {
        this.dashboardAnalyticsService = dashboardAnalyticsService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardAnalyticsService.summary(principal);
    }
}

