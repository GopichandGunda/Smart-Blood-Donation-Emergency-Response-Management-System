package com.bloodmanagement.model;

import java.time.LocalDateTime;

public record Notification(
        long id,
        long userId,
        String message,
        LocalDateTime createdAt,
        boolean read) {
}
