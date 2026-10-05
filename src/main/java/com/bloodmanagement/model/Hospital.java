package com.bloodmanagement.model;

public record Hospital(
        long id,
        String name,
        String address,
        String city,
        String phone,
        String email,
        String emergencyContact,
        boolean active) {
}
