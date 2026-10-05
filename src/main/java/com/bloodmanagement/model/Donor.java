package com.bloodmanagement.model;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.DonorStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Donor(
        long id,
        String fullName,
        int age,
        String gender,
        BloodGroup bloodGroup,
        String phone,
        String email,
        String address,
        String city,
        LocalDate dateOfBirth,
        LocalDate lastDonationDate,
        int totalDonations,
        boolean available,
        boolean eligible,
        DonorStatus status,
        LocalDateTime registrationDate) {
}
