package com.bloodmanagement.service;

import com.bloodmanagement.dao.DonationDAO;
import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.enums.DonorStatus;
import com.bloodmanagement.model.Donation;
import com.bloodmanagement.util.ValidationUtil;
import java.time.LocalDate;

public final class DonationService {
    private final DonationDAO donations;
    private final DonorDAO donors;
    private final DonorEligibilityService eligibility;

    public DonationService(DonationDAO donations, DonorDAO donors, DonorEligibilityService eligibility) {
        this.donations = donations;
        this.donors = donors;
        this.eligibility = eligibility;
    }

    public void record(long donorId, int units, long bloodBankId, long staffId) {
        ValidationUtil.positive(units, "Donation units");
        var donor = donors.findById(donorId).orElseThrow(() -> new IllegalArgumentException("Donor not found."));
        eligibility.requireEligible(donor, LocalDate.now());
        if (donor.status() != DonorStatus.ACTIVE) {
            throw new IllegalArgumentException("Inactive donors cannot donate.");
        }
        donations.record(new Donation(0, donorId, donor.bloodGroup(), LocalDate.now(), units, bloodBankId, staffId, true));
    }

    public java.util.List<Donation> historyForDonor(long donorId) {
        return donations.findForDonor(donorId);
    }
}
