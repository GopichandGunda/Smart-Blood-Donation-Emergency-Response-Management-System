package com.bloodmanagement.model;

public record BloodBank(
        long id,
        String name,
        String address,
        String city,
        String phone,
        String email,
        int storageCapacity,
        int currentStock,
        boolean active) {
}
