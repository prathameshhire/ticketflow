package com.ticketflow.alert;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Alert> findByRecipientIdAndReadFlagFalse(Long recipientId);

    Optional<Alert> findByIdAndRecipientId(Long id, Long recipientId);
}
