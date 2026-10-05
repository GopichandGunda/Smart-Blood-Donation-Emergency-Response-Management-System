package com.bloodmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.DonorStatus;
import com.bloodmanagement.model.Donor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DonorMatchingServiceTest {
    private final BloodCompatibilityService compatibility = new BloodCompatibilityService();
    private final DonorEligibilityService eligibility = new DonorEligibilityService(new DonorDAO());
    private final DonorMatchingService service = new DonorMatchingService(new DonorDAO(), compatibility, eligibility);

    @Test
    void ranksExactLocalMatchAheadOfCompatibleDistantDonor() {
        Donor exactLocal = donor(1, BloodGroup.A_POSITIVE, "Hyderabad", null);
        Donor compatibleFar = donor(2, BloodGroup.O_NEGATIVE, "Pune", null);

        List<DonorMatchingService.MatchResult> matches =
                service.rankCandidates(List.of(compatibleFar, exactLocal), BloodGroup.A_POSITIVE, "Hyderabad");

        assertEquals(2, matches.size());
        assertEquals(1, matches.get(0).donor().id());
        assertTrue(matches.get(0).score() > matches.get(matches.size() - 1).score());
    }

    @Test
    void excludesIncompatibleUnavailableAndRecentlyDonatedCandidates() {
        List<DonorMatchingService.MatchResult> matches = service.rankCandidates(List.of(
                donor(1, BloodGroup.AB_POSITIVE, "Hyderabad", null),
                donor(2, BloodGroup.O_NEGATIVE, "Hyderabad", LocalDate.now().minusDays(20)),
                new Donor(3, "Unavailable", 25, "Other", BloodGroup.O_NEGATIVE, "+1 555 555 0103",
                        "unavailable@example.org", "1 Main Street", "Hyderabad", LocalDate.now().minusYears(25),
                        null, 0, false, true, DonorStatus.ACTIVE, LocalDateTime.now())),
                BloodGroup.O_POSITIVE, "Hyderabad");

        assertTrue(matches.isEmpty());
    }

    private Donor donor(long id, BloodGroup group, String city, LocalDate lastDonation) {
        return new Donor(id, "Donor " + id, 25, "Unspecified", group, "+1 555 555 010" + id,
                "donor" + id + "@example.org", "1 Main Street", city, LocalDate.now().minusYears(25),
                lastDonation, 0, true, true, DonorStatus.ACTIVE, LocalDateTime.now());
    }
}
