package com.ticketflow.auth;

import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticketflow.auth.dto.AuthResponse;
import com.ticketflow.auth.dto.CurrentUserResponse;
import com.ticketflow.auth.dto.LoginRequest;
import com.ticketflow.auth.dto.RegisterRequest;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRepository;
import com.ticketflow.user.UserRole;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("A user with this email already exists.");
        }

        UserRole role = resolveRegistrationRole(request.role());
        User user = User.create(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                role
        );
        User savedUser = userRepository.save(user);
        return authResponse(UserPrincipal.from(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        return authResponse(UserPrincipal.from(user));
    }

    public CurrentUserResponse currentUser(UserPrincipal principal) {
        return CurrentUserResponse.from(principal);
    }

    private AuthResponse authResponse(UserPrincipal principal) {
        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, "Bearer", CurrentUserResponse.from(principal));
    }

    private UserRole resolveRegistrationRole(UserRole requestedRole) {
        UserRole role = requestedRole == null ? UserRole.CUSTOMER : requestedRole;
        if (role == UserRole.CUSTOMER) {
            return role;
        }

        if (role == UserRole.ADMIN && userRepository.count() == 0) {
            return role;
        }

        throw new AccessDeniedException("Only customers can self-register after the initial admin account.");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

