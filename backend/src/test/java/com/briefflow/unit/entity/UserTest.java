package com.briefflow.unit.entity;

import com.briefflow.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void should_createUser_when_allFieldsProvided() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");

        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashedpassword", user.getPassword());
        assertTrue(user.isActive());
    }
}
