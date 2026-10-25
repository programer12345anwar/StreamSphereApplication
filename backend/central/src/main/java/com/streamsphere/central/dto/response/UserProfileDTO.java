package com.streamsphere.central.dto.response;

import java.util.UUID;

import com.streamsphere.central.entity.AppUser;

public record UserProfileDTO(UUID id, String name, String email, String role) {
    public static UserProfileDTO from(AppUser user) {
        return new UserProfileDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
