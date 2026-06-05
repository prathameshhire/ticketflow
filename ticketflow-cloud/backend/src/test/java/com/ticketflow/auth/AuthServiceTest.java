package com.ticketflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketflow.auth.dto.LoginRequest;
import com.ticketflow.auth.dto.RegisterRequest;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRepository;
import com.ticketflow.user.UserRole;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCustomerHashesPasswordAndReturnsToken() {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 42L);
            return saved;
        });
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

        var response = authService.register(new RegisterRequest("New User", "NEW@EXAMPLE.COM", "password123", null));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("new@example.com");
        assertThat(response.user().role()).isEqualTo(UserRole.CUSTOMER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Taken", "taken@example.com", "password123", null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void loginRejectsInvalidPassword() {
        User user = User.create("Existing User", "existing@example.com", "hash", UserRole.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("existing@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("bad-password", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("existing@example.com", "bad-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void onlyFirstAccountCanSelfRegisterAsAdmin() {
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(userRepository.count()).thenReturn(3L);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Admin", "admin@example.com", "password123", UserRole.ADMIN)))
                .isInstanceOf(AccessDeniedException.class);
    }
}

