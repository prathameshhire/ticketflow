package com.ticketflow.alert;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketflow.alert.dto.AlertResponse;
import com.ticketflow.alert.dto.ReadAllAlertsResponse;
import com.ticketflow.auth.UserPrincipal;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertResponse> listAlerts(@AuthenticationPrincipal UserPrincipal principal) {
        return alertService.listAlerts(principal);
    }

    @PatchMapping("/{id}/read")
    public AlertResponse markRead(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return alertService.markRead(principal, id);
    }

    @PatchMapping("/read-all")
    public ReadAllAlertsResponse markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        return alertService.markAllRead(principal);
    }
}

