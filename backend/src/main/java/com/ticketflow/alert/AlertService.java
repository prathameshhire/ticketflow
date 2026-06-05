package com.ticketflow.alert;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticketflow.alert.dto.AlertResponse;
import com.ticketflow.alert.dto.ReadAllAlertsResponse;
import com.ticketflow.auth.UserPrincipal;
import com.ticketflow.common.ResourceNotFoundException;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> listAlerts(UserPrincipal principal) {
        return alertRepository.findByRecipientIdOrderByCreatedAtDesc(principal.id())
                .stream()
                .map(AlertResponse::from)
                .toList();
    }

    @Transactional
    public AlertResponse markRead(UserPrincipal principal, Long id) {
        Alert alert = alertRepository.findByIdAndRecipientId(id, principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found."));
        alert.setReadFlag(true);
        return AlertResponse.from(alertRepository.save(alert));
    }

    @Transactional
    public ReadAllAlertsResponse markAllRead(UserPrincipal principal) {
        List<Alert> unreadAlerts = alertRepository.findByRecipientIdAndReadFlagFalse(principal.id());
        unreadAlerts.forEach(alert -> alert.setReadFlag(true));
        alertRepository.saveAll(unreadAlerts);
        return new ReadAllAlertsResponse(unreadAlerts.size());
    }
}

