package com.bloodmanagement.service;

import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.enums.DonorStatus;
import com.bloodmanagement.exception.IneligibleDonorException;
import com.bloodmanagement.model.Donor;
import java.time.LocalDate;
import java.time.Period;

public final class DonorEligibilityService {
    private static final int MINIMUM_AGE = 18;
    private static final int DONATION_INTERVAL_DAYS = 90;
    private final DonorDAO donors;

    public DonorEligibilityService(DonorDAO donors) {
        this.donors = donors;
    }

    public boolean checkEligibility(Donor donor, LocalDate date) {
        return donor.status() == DonorStatus.ACTIVE
                && donor.available()
                && Period.between(donor.dateOfBirth(), date).getYears() >= MINIMUM_AGE
                && calculateNextEligibleDate(donor).compareTo(date) <= 0;
    }

    public LocalDate calculateNextEligibleDate(Donor donor) {
        if (donor.lastDonationDate() == null) {
            return donor.dateOfBirth().plusYears(MINIMUM_AGE);
        }
        return donor.lastDonationDate().plusDays(DONATION_INTERVAL_DAYS);
    }

    public void updateEligibilityStatus(Donor donor) {
        boolean eligible = checkEligibility(donor, LocalDate.now());
        donors.updateEligibility(donor.id(), eligible);
    }

    public void requireEligible(Donor donor, LocalDate date) {
        if (!checkEligibility(donor, date)) {
            throw new IneligibleDonorException(
                    "Donor is not eligible. Next eligible date: " + calculateNextEligibleDate(donor) + ".");
        }
    }
}
