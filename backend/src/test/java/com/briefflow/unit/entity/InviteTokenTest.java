package com.briefflow.unit.entity;

import com.briefflow.entity.InviteToken;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InviteTokenTest {

    @Test
    void should_returnTrue_when_tokenIsExpired() {
        InviteToken token = new InviteToken();
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        token.setUsed(false);

        assertTrue(token.isExpired());
        assertFalse(token.isUsable());
    }

    @Test
    void should_returnFalse_when_tokenIsNotExpired() {
        InviteToken token = new InviteToken();
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(false);

        assertFalse(token.isExpired());
        assertTrue(token.isUsable());
    }

    @Test
    void should_returnFalse_when_tokenIsUsed() {
        InviteToken token = new InviteToken();
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(true);

        assertFalse(token.isUsable());
    }
}
