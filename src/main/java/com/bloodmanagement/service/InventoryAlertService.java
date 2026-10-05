package com.bloodmanagement.service;

import com.bloodmanagement.enums.BloodGroup;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class InventoryAlertService {
    private static final Map<BloodGroup, Integer> MINIMUM_STOCK = minimumStock();

    public List<Alert> evaluate(Map<String, Integer> stock) {
        return java.util.Arrays.stream(BloodGroup.values())
                .map(group -> {
                    int available = stock.getOrDefault(group.getDisplayName(), 0);
                    int minimum = MINIMUM_STOCK.get(group);
                    if (available == 0) {
                        return new Alert(group, available, minimum, Severity.CRITICAL, "CRITICAL BLOOD SHORTAGE");
                    }
                    if (available < minimum) {
                        return new Alert(group, available, minimum, Severity.LOW, "LOW BLOOD STOCK ALERT");
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public int minimumStock(BloodGroup group) {
        return MINIMUM_STOCK.get(group);
    }

    private static Map<BloodGroup, Integer> minimumStock() {
        Map<BloodGroup, Integer> thresholds = new EnumMap<>(BloodGroup.class);
        for (BloodGroup group : BloodGroup.values()) {
            thresholds.put(group, 10);
        }
        return Map.copyOf(thresholds);
    }

    public enum Severity {
        LOW,
        CRITICAL
    }

    public record Alert(BloodGroup bloodGroup, int currentStock, int minimumStock, Severity severity, String message) {
    }
}
