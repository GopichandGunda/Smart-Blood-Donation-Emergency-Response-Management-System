package com.bloodmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bloodmanagement.enums.BloodGroup;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InventoryAlertServiceTest {
    private final InventoryAlertService service = new InventoryAlertService();

    @Test
    void reportsLowStockBelowConfiguredMinimum() {
        var alerts = service.evaluate(Map.of("A+", 4));

        var alert = alerts.stream().filter(item -> item.bloodGroup() == BloodGroup.A_POSITIVE).findFirst().orElseThrow();
        assertEquals(InventoryAlertService.Severity.LOW, alert.severity());
        assertEquals(10, alert.minimumStock());
    }

    @Test
    void reportsCriticalShortageWhenStockIsZero() {
        var alert = service.evaluate(Map.of()).stream()
                .filter(item -> item.bloodGroup() == BloodGroup.O_NEGATIVE).findFirst().orElseThrow();

        assertEquals(InventoryAlertService.Severity.CRITICAL, alert.severity());
        assertEquals("CRITICAL BLOOD SHORTAGE", alert.message());
    }

    @Test
    void doesNotAlertAtMinimumStock() {
        assertEquals(0, service.evaluate(Map.of("A+", 10)).stream()
                .filter(item -> item.bloodGroup() == BloodGroup.A_POSITIVE).count());
    }
}
