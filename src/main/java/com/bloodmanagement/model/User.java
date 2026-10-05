package com.bloodmanagement.model;

import com.bloodmanagement.enums.UserRole;
import java.time.LocalDateTime;

public record User(
        long id,
        String username,
        String passwordHash,
        UserRole role,
        boolean active,
        LocalDateTime createdAt) {
}
