package com.bloodmanagement.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.dao.BloodUnitDAO;
import com.bloodmanagement.dao.DonationDAO;
import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.dao.EmergencyRequestDAO;
import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.BloodUnitStatus;
import com.bloodmanagement.enums.EmergencyLevel;
import com.bloodmanagement.model.BloodUnit;
import com.bloodmanagement.model.EmergencyRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ServiceValidationTest {
    @Test
    void rejectsEmergencyRequestWithZeroUnitsBeforeDatabaseAccess() {
        EmergencyRequestService service = new EmergencyRequestService(new EmergencyRequestDAO(), new AuditLogDAO());
        EmergencyRequest invalid = new EmergencyRequest(0, 1, "Patient", "P1", BloodGroup.O_POSITIVE,
                0, EmergencyLevel.CRITICAL, LocalDate.now(), LocalDate.now(), LocalTime.NOON,
                "Hospital", "+1 555 555 0100", null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(invalid, 1));
    }

    @Test
    void rejectsNonPositiveDonationUnitsBeforeDatabaseAccess() {
        DonorDAO donors = new DonorDAO();
        DonorEligibilityService eligibility = new DonorEligibilityService(donors);
        DonationService service = new DonationService(new DonationDAO(), donors, eligibility);

        assertThrows(IllegalArgumentException.class, () -> service.record(1, 0, 1, 1));
    }

    @Test
    void rejectsNonPositiveBloodStockBeforeDatabaseAccess() {
        BloodInventoryService service = new BloodInventoryService(new BloodUnitDAO(), new AuditLogDAO(),
                new InventoryAlertService(), new BloodCompatibilityService());
        BloodUnit invalid = new BloodUnit(0, BloodGroup.A_POSITIVE, LocalDate.now(),
                LocalDate.now().plusDays(35), 0, "Shelf A", 1, BloodUnitStatus.AVAILABLE);

        assertThrows(IllegalArgumentException.class, () -> service.add(invalid, 1));
    }
}
