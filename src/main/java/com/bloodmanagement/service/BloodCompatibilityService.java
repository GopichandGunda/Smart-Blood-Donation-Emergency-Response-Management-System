package com.bloodmanagement.service;

import com.bloodmanagement.enums.BloodGroup;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class BloodCompatibilityService {
    private final Map<BloodGroup, Set<BloodGroup>> compatibleDonorsByRecipient;

    public BloodCompatibilityService() {
        compatibleDonorsByRecipient = new EnumMap<>(BloodGroup.class);
        compatibleDonorsByRecipient.put(BloodGroup.A_POSITIVE,
                Set.of(BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE));
        compatibleDonorsByRecipient.put(BloodGroup.A_NEGATIVE,
                Set.of(BloodGroup.A_NEGATIVE, BloodGroup.O_NEGATIVE));
        compatibleDonorsByRecipient.put(BloodGroup.B_POSITIVE,
                Set.of(BloodGroup.B_POSITIVE, BloodGroup.B_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE));
        compatibleDonorsByRecipient.put(BloodGroup.B_NEGATIVE,
                Set.of(BloodGroup.B_NEGATIVE, BloodGroup.O_NEGATIVE));
        compatibleDonorsByRecipient.put(BloodGroup.AB_POSITIVE, Set.of(BloodGroup.values()));
        compatibleDonorsByRecipient.put(BloodGroup.AB_NEGATIVE,
                Set.of(BloodGroup.AB_NEGATIVE, BloodGroup.A_NEGATIVE, BloodGroup.B_NEGATIVE, BloodGroup.O_NEGATIVE));
        compatibleDonorsByRecipient.put(BloodGroup.O_POSITIVE,
                Set.of(BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE));
        compatibleDonorsByRecipient.put(BloodGroup.O_NEGATIVE, Set.of(BloodGroup.O_NEGATIVE));
    }

    public boolean isCompatible(BloodGroup donor, BloodGroup recipient) {
        return compatibleDonorsByRecipient.getOrDefault(recipient, Set.of()).contains(donor);
    }

    public Set<BloodGroup> compatibleDonorGroups(BloodGroup recipient) {
        return compatibleDonorsByRecipient.getOrDefault(recipient, Set.of());
    }
}
