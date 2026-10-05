package com.bloodmanagement.model;

import com.bloodmanagement.enums.BloodGroup;
import java.time.LocalDate;

public record Donation(
        long id,
        long donorId,
        BloodGroup bloodGroup,
        LocalDate donationDate,
        int units,
        long bloodBankId,
        long staffId,
        boolean verified) {
}
