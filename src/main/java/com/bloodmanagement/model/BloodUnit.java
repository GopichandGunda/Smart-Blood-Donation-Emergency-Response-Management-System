package com.bloodmanagement.model;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.BloodUnitStatus;
import java.time.LocalDate;

public record BloodUnit(
        long id,
        BloodGroup bloodGroup,
        LocalDate collectionDate,
        LocalDate expiryDate,
        int quantity,
        String storageLocation,
        long bloodBankId,
        BloodUnitStatus status) {
}
