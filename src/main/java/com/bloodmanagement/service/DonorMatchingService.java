package com.bloodmanagement.service;

import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.DonorStatus;
import com.bloodmanagement.model.Donor;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class DonorMatchingService {
    private final DonorDAO donors;
    private final BloodCompatibilityService compatibility;
    private final DonorEligibilityService eligibility;

    public DonorMatchingService(DonorDAO donors, BloodCompatibilityService compatibility,
            DonorEligibilityService eligibility) {
        this.donors = donors;
        this.compatibility = compatibility;
        this.eligibility = eligibility;
    }

    public List<MatchResult> findMatches(BloodGroup required, String city) {
        return rankCandidates(donors.findAll(), required, city);
    }

    public List<MatchResult> rankCandidates(List<Donor> candidates, BloodGroup required, String city) {
        return candidates.stream()
                .filter(donor -> donor.status() == DonorStatus.ACTIVE)
                .filter(donor -> donor.available())
                .filter(donor -> compatibility.isCompatible(donor.bloodGroup(), required))
                .filter(donor -> eligibility.checkEligibility(donor, LocalDate.now()))
                .map(donor -> new MatchResult(donor, score(donor, required, city)))
                .sorted(Comparator.comparingInt(MatchResult::score).reversed()
                        .thenComparing(result -> result.donor().fullName()))
                .toList();
    }

    public int score(Donor donor, BloodGroup required, String city) {
        if (!compatibility.isCompatible(donor.bloodGroup(), required)
                || donor.status() != DonorStatus.ACTIVE || !donor.available()
                || !eligibility.checkEligibility(donor, LocalDate.now())) {
            return 0;
        }
        int score = donor.bloodGroup() == required ? 40 : 30;
        score += 20;
        score += 15;
        if (city != null && !city.isBlank() && donor.city().equalsIgnoreCase(city.trim())) {
            score += 15;
        }
        if (donor.lastDonationDate() == null
                || donor.lastDonationDate().plusDays(180).isBefore(LocalDate.now())) {
            score += 10;
        } else if (donor.lastDonationDate().plusDays(120).isBefore(LocalDate.now())) {
            score += 7;
        } else {
            score += 5;
        }
        return score;
    }

    public record MatchResult(Donor donor, int score) {
    }
}
