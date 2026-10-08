package com.streamsphere.central.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserDTO(
        UUID id,
        String name,
        String email,
        Long phoneNumber,
        String role,
        LocalDateTime createdAt
) {}


