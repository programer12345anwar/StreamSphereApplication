package com.streamsphere.central.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalServiceTokenValidator {
    private final String expectedToken;

    public InternalServiceTokenValidator(
            @Value("${central.internal-service-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void validate(String suppliedToken) {
        if (expectedToken == null || expectedToken.isBlank()
                || suppliedToken == null || suppliedToken.isBlank()
                || !MessageDigest.isEqual(
                        expectedToken.getBytes(StandardCharsets.UTF_8),
                        suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Internal service authentication is required.");
        }
    }
}
