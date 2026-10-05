package com.bloodmanagement.model;

import java.time.LocalDateTime;

public record DonorMatch(
        long id,
        long emergencyRequestId,
        long donorId,
        int matchingScore,
        LocalDateTime matchedAt,
        boolean accepted) {
}
