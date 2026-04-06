package com.briefflow.unit.security;

import com.briefflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
            "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256",
            900000L,
            604800000L
        );
    }

    @Test
    void should_generateAccessToken_when_validInput() {
        String token = jwtService.generateAccessToken(1L, "user@example.com");

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void should_extractUserId_when_validToken() {
        String token = jwtService.generateAccessToken(42L, "user@example.com");

        Long userId = jwtService.extractUserId(token);

        assertEquals(42L, userId);
    }

    @Test
    void should_extractEmail_when_validToken() {
        String token = jwtService.generateAccessToken(1L, "user@example.com");

        String email = jwtService.extractEmail(token);

        assertEquals("user@example.com", email);
    }

    @Test
    void should_validateToken_when_tokenIsValid() {
        String token = jwtService.generateAccessToken(1L, "user@example.com");

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void should_failValidation_when_tokenIsTampered() {
        String token = jwtService.generateAccessToken(1L, "user@example.com");
        String tampered = token + "tampered";

        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    void should_failValidation_when_tokenIsGarbage() {
        assertFalse(jwtService.isTokenValid("not.a.jwt"));
    }

    @Test
    void should_returnExpiresInMillis() {
        long expiresIn = jwtService.getAccessExpirationMs();

        assertEquals(900000L, expiresIn);
    }
}
