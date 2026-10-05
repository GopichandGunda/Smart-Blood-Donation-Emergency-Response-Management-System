package com.bloodmanagement.model;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.TransferStatus;
import java.time.LocalDate;

public record BloodTransfer(
        long id,
        long sourceBloodBankId,
        long destinationBloodBankId,
        BloodGroup bloodGroup,
        int units,
        LocalDate requestDate,
        TransferStatus status,
        LocalDate transferDate,
        long staffId) {
}
