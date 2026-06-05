package com.ticketflow.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketflow.support.TestFixtures;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRole;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Test
    void markReadOnlyUpdatesCurrentUsersAlert() {
        User customer = TestFixtures.user(1L, UserRole.CUSTOMER);
        Alert alert = TestFixtures.alert(5L, customer, AlertType.COMMENT);
        when(alertRepository.findByIdAndRecipientId(5L, 1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(alert)).thenReturn(alert);

        AlertService service = new AlertService(alertRepository);
        var response = service.markRead(TestFixtures.principal(customer), 5L);

        assertThat(response.readFlag()).isTrue();
        verify(alertRepository).findByIdAndRecipientId(5L, 1L);
    }

    @Test
    void markAllReadReturnsUpdatedCount() {
        User agent = TestFixtures.user(2L, UserRole.AGENT);
        List<Alert> alerts = List.of(
                TestFixtures.alert(1L, agent, AlertType.ASSIGNMENT),
                TestFixtures.alert(2L, agent, AlertType.COMMENT)
        );
        when(alertRepository.findByRecipientIdAndReadFlagFalse(2L)).thenReturn(alerts);
        when(alertRepository.saveAll(alerts)).thenReturn(alerts);

        AlertService service = new AlertService(alertRepository);
        var response = service.markAllRead(TestFixtures.principal(agent));

        assertThat(response.updatedCount()).isEqualTo(2);
        assertThat(alerts).allMatch(Alert::isReadFlag);
    }
}

