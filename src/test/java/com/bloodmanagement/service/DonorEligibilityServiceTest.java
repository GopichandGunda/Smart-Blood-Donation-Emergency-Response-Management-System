package com.bloodmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.DonorStatus;
import com.bloodmanagement.model.Donor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DonorEligibilityServiceTest {
    private final DonorEligibilityService service = new DonorEligibilityService(new DonorDAO());

    @Test
    void considersAdultAvailableActiveDonorEligibleWithoutDonationHistory() {
        assertTrue(service.checkEligibility(donor(true, DonorStatus.ACTIVE, null), LocalDate.now()));
    }

    @Test
    void rejectsDonorWithinDonationInterval() {
        Donor recent = donor(true, DonorStatus.ACTIVE, LocalDate.now().minusDays(30));
        assertFalse(service.checkEligibility(recent, LocalDate.now()));
        assertEquals(LocalDate.now().plusDays(60), service.calculateNextEligibleDate(recent));
    }

    @Test
    void rejectsUnavailableAndInactiveDonors() {
        assertFalse(service.checkEligibility(donor(false, DonorStatus.ACTIVE, null), LocalDate.now()));
        assertFalse(service.checkEligibility(donor(true, DonorStatus.INACTIVE, null), LocalDate.now()));
    }

    private Donor donor(boolean available, DonorStatus status, LocalDate lastDonation) {
        return new Donor(1, "Test Donor", 25, "Unspecified", BloodGroup.O_NEGATIVE,
                "+1 555 555 0100", "donor@example.org", "1 Main Street", "Hyderabad",
                LocalDate.now().minusYears(25), lastDonation, 0, available, true, status, LocalDateTime.now());
    }
}
