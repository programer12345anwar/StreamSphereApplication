package com.streamsphere.central.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.streamsphere.central.entity.AppUser;
import com.streamsphere.central.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserService userService;

    private static final String SECRET = "testsecretkeytestsecretkeytestsecretkey1234567890";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        userService = mock(UserService.class);
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "userService", userService);
    }

    @Test
    void generateAndDecryptRoundTrip() {
        String token = jwtUtil.generateToken("user@example.com");
        assertEquals("user@example.com", jwtUtil.decryptToken(token));
    }

    @Test
    void isValidTokenTrueWhenUserExists() {
        String token = jwtUtil.generateToken("user@example.com");
        when(userService.getUserByEmail("user@example.com")).thenReturn(new AppUser());
        assertTrue(jwtUtil.isValidToken(token));
    }

    @Test
    void isValidTokenFalseWhenUserMissing() {
        String token = jwtUtil.generateToken("ghost@example.com");
        when(userService.getUserByEmail("ghost@example.com")).thenReturn(null);
        assertFalse(jwtUtil.isValidToken(token));
    }

    @Test
    void isValidTokenFalseForGarbageToken() {
        assertFalse(jwtUtil.isValidToken("not.a.jwt"));
    }
}

