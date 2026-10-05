package com.bloodmanagement.model;

import java.time.LocalDateTime;

public record AuditLog(
        long id,
        Long userId,
        String action,
        LocalDateTime timestamp,
        String description) {
}
