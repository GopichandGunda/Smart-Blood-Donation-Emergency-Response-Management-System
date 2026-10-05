package com.bloodmanagement.model;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.EmergencyLevel;
import com.bloodmanagement.enums.RequestStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record EmergencyRequest(
        long id,
        long hospitalId,
        String patientName,
        String patientId,
        BloodGroup bloodGroupRequired,
        int unitsRequired,
        EmergencyLevel emergencyLevel,
        LocalDate requestDate,
        LocalDate requiredDate,
        LocalTime requiredTime,
        String hospitalLocation,
        String contactNumber,
        RequestStatus status,
        Long assignedDonorId,
        Long approvedBy) {
}
