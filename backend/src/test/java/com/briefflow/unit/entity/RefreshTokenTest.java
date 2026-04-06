package com.briefflow.unit.entity;

import com.briefflow.entity.RefreshToken;
import com.briefflow.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void should_createRefreshToken_when_allFieldsProvided() {
        User user = new User();
        user.setId(1L);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("some-uuid-token");
        token.setExpiresAt(LocalDateTime.now().plusDays(7));

        assertEquals(user, token.getUser());
        assertEquals("some-uuid-token", token.getToken());
        assertFalse(token.isRevoked());
    }

    @Test
    void should_beExpired_when_expiresAtIsInPast() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(LocalDateTime.now().minusHours(1));

        assertTrue(token.isExpired());
    }

    @Test
    void should_notBeExpired_when_expiresAtIsInFuture() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(LocalDateTime.now().plusDays(7));

        assertFalse(token.isExpired());
    }
}
