package com.ticketflow.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.user.UserRole;

import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String secret;
    private final long expirationMinutes;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${ticketflow.security.jwt-secret}") String secret,
            @Value("${ticketflow.security.jwt-expiration-minutes:120}") long expirationMinutes
    ) {
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    @PostConstruct
    void validateConfiguration() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("ticketflow.security.jwt-secret must be configured.");
        }
    }

    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", principal.getUsername());
        payload.put("userId", principal.id());
        payload.put("name", principal.name());
        payload.put("role", principal.role().name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signaturePart = sign(headerPart + "." + payloadPart);

        return headerPart + "." + payloadPart + "." + signaturePart;
    }

    public JwtClaims parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("Invalid or expired token.");
        }

        String signedContent = parts[0] + "." + parts[1];
        String expectedSignature = sign(signedContent);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidTokenException("Invalid or expired token.");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        Instant expiresAt = Instant.ofEpochSecond(numberClaim(payload, "exp"));
        if (!expiresAt.isAfter(Instant.now(clock))) {
            throw new InvalidTokenException("Invalid or expired token.");
        }

        return new JwtClaims(
                numberClaim(payload, "userId"),
                stringClaim(payload, "sub"),
                stringClaim(payload, "name"),
                UserRole.valueOf(stringClaim(payload, "role")),
                Instant.ofEpochSecond(numberClaim(payload, "iat")),
                expiresAt
        );
    }

    private String encodeJson(Map<String, Object> values) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(values);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new InvalidTokenException("Unable to create token.", exception);
        }
    }

    private Map<String, Object> decodeJson(String tokenPart) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(tokenPart);
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            throw new InvalidTokenException("Invalid or expired token.", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(key);
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new InvalidTokenException("Unable to process token.", exception);
        }
    }

    private String stringClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw new InvalidTokenException("Invalid or expired token.");
    }

    private Long numberClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw new InvalidTokenException("Invalid or expired token.");
    }
}

