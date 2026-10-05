package com.bloodmanagement.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bloodmanagement.enums.BloodGroup;
import org.junit.jupiter.api.Test;

class BloodCompatibilityServiceTest {
    private final BloodCompatibilityService service = new BloodCompatibilityService();

    @Test
    void acceptsConservativeRhNegativeToRhPositiveRedCellMatch() {
        assertTrue(service.isCompatible(BloodGroup.O_NEGATIVE, BloodGroup.A_POSITIVE));
    }

    @Test
    void rejectsRhPositiveDonorForRhNegativeRecipient() {
        assertFalse(service.isCompatible(BloodGroup.O_POSITIVE, BloodGroup.A_NEGATIVE));
    }

    @Test
    void rejectsABDonorForORecipient() {
        assertFalse(service.isCompatible(BloodGroup.AB_POSITIVE, BloodGroup.O_POSITIVE));
    }

    @Test
    void acceptsOnlyOnegativeForOnegativeRecipient() {
        assertTrue(service.isCompatible(BloodGroup.O_NEGATIVE, BloodGroup.O_NEGATIVE));
        assertFalse(service.isCompatible(BloodGroup.A_NEGATIVE, BloodGroup.O_NEGATIVE));
    }
}
