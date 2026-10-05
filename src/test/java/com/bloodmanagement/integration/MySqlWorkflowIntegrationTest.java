package com.bloodmanagement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.dao.BloodBankDAO;
import com.bloodmanagement.dao.BloodTransferDAO;
import com.bloodmanagement.dao.BloodUnitDAO;
import com.bloodmanagement.dao.DonationDAO;
import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.dao.EmergencyRequestDAO;
import com.bloodmanagement.dao.HospitalDAO;
import com.bloodmanagement.dao.UserDAO;
import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.BloodUnitStatus;
import com.bloodmanagement.enums.DonorStatus;
import com.bloodmanagement.enums.EmergencyLevel;
import com.bloodmanagement.enums.RequestStatus;
import com.bloodmanagement.enums.TransferStatus;
import com.bloodmanagement.model.BloodBank;
import com.bloodmanagement.model.BloodTransfer;
import com.bloodmanagement.model.BloodUnit;
import com.bloodmanagement.model.Donor;
import com.bloodmanagement.model.EmergencyRequest;
import com.bloodmanagement.model.Hospital;
import com.bloodmanagement.service.AccountService;
import com.bloodmanagement.service.AuthenticationService;
import com.bloodmanagement.service.BloodBankService;
import com.bloodmanagement.service.BloodCompatibilityService;
import com.bloodmanagement.service.BloodInventoryService;
import com.bloodmanagement.service.BloodTransferService;
import com.bloodmanagement.service.DonationService;
import com.bloodmanagement.service.DonorEligibilityService;
import com.bloodmanagement.service.DonorMatchService;
import com.bloodmanagement.service.DonorMatchingService;
import com.bloodmanagement.service.DonorService;
import com.bloodmanagement.service.EmergencyRequestService;
import com.bloodmanagement.service.HospitalService;
import com.bloodmanagement.service.InventoryAlertService;
import com.bloodmanagement.util.PasswordUtil;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "BLOOD_DB_INTEGRATION", matches = "true")
class MySqlWorkflowIntegrationTest {
    private final UserDAO users = new UserDAO();
    private final DonorDAO donors = new DonorDAO();
    private final AuditLogDAO audit = new AuditLogDAO();
    private final BloodUnitDAO units = new BloodUnitDAO();
    private final BloodCompatibilityService compatibility = new BloodCompatibilityService();
    private final DonorEligibilityService eligibility = new DonorEligibilityService(donors);
    private final BloodInventoryService inventory = new BloodInventoryService(
            units, audit, new InventoryAlertService(), compatibility);

    @Test
    void authenticatesRegistersMatchesReservesIssuesAndTransfersStock() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        char[] staffPassword = "integration-admin-password".toCharArray();
        long staffId = users.createAdmin("admin_" + suffix, PasswordUtil.hash(staffPassword)).id();
        String donorUsername = "donor_" + suffix;
        char[] donorPassword = "integration-test-password".toCharArray();

        AuthenticationService authentication = new AuthenticationService(users, audit);
        assertEquals(staffId, authentication.authenticate("admin_" + suffix, staffPassword).id());
        java.util.Arrays.fill(staffPassword, '\0');

        DonorService donorService = new DonorService(donors, audit, eligibility);
        Donor candidate = donor("Match " + suffix, "match_" + suffix + "@example.org",
                uniquePhone(), BloodGroup.A_POSITIVE, "Hyderabad");
        long donorId = donorService.register(candidate, donorUsername, donorPassword, staffId);
        var donorAccount = users.findByUsername(donorUsername).orElseThrow();
        assertEquals(donorId, donors.findByUserId(donorAccount.id()).orElseThrow().id());

        HospitalService hospitals = new HospitalService(new HospitalDAO());
        long hospitalId = hospitals.create(new Hospital(0, "Integration Hospital " + suffix,
                "1 Integration Road", "Hyderabad", "+1 555 700 2001",
                "hospital_" + suffix + "@example.org", "+1 555 700 2002", true));
        BloodBankService banks = new BloodBankService(new BloodBankDAO());
        long requestBankId = banks.create(bank("Request Bank " + suffix, suffix + "@request.example.org"));
        BloodBank requestBank = banks.findAll().stream().filter(value -> value.id() == requestBankId).findFirst().orElseThrow();

        inventory.add(unit(BloodGroup.A_POSITIVE, 3, requestBank), staffId);
        EmergencyRequest request = new EmergencyRequest(0, hospitalId, "Integration Patient " + suffix, "PAT-" + suffix,
                BloodGroup.A_POSITIVE, 1, EmergencyLevel.CRITICAL, LocalDate.now(), LocalDate.now(),
                LocalTime.NOON, "Hyderabad", "+1 555 700 3001", RequestStatus.CREATED, null, staffId);
        long requestId = new EmergencyRequestService(new EmergencyRequestDAO(), audit).create(request, staffId);

        DonorMatchingService matching = new DonorMatchingService(donors, compatibility, eligibility);
        var ranked = matching.findMatches(BloodGroup.A_POSITIVE, "Hyderabad");
        assertTrue(ranked.stream().anyMatch(result -> result.donor().id() == donorId));
        assertTrue(ranked.get(0).score() <= 100);

        DonorMatchService matchService = new DonorMatchService(
                new com.bloodmanagement.dao.DonorMatchDAO(), audit);
        matchService.save(requestId, ranked, staffId);
        new EmergencyRequestDAO().updateStatus(requestId, RequestStatus.MATCHING);
        var offer = matchService.findOffers(donorAccount.id()).stream()
                .filter(value -> value.match().emergencyRequestId() == requestId).findFirst().orElseThrow();
        matchService.respond(offer.match().id(), donorAccount.id(), true);
        assertEquals(RequestStatus.DONOR_FOUND, new EmergencyRequestDAO().findAll().stream()
                .filter(value -> value.id() == requestId).findFirst().orElseThrow().status());

        inventory.reserve(requestId, BloodGroup.A_POSITIVE, BloodGroup.A_POSITIVE, 1, staffId);
        inventory.issue(requestId, staffId);
        assertEquals(RequestStatus.FULFILLED, new EmergencyRequestDAO().findAll().stream()
                .filter(value -> value.id() == requestId).findFirst().orElseThrow().status());
        assertTrue(units.findAll().stream().anyMatch(value -> value.status() == BloodUnitStatus.ISSUED
                && value.quantity() == 1));

        long sourceId = banks.create(bank("Transfer Source " + suffix, suffix + "@source.example.org"));
        long destinationId = banks.create(bank("Transfer Destination " + suffix, suffix + "@destination.example.org"));
        BloodBank source = banks.findAll().stream().filter(value -> value.id() == sourceId).findFirst().orElseThrow();
        inventory.add(unit(BloodGroup.O_NEGATIVE, 4, source), staffId);
        BloodTransferService transfers = new BloodTransferService(new BloodTransferDAO(), audit);
        long transferId = transfers.create(new BloodTransfer(0, sourceId, destinationId, BloodGroup.O_NEGATIVE,
                2, LocalDate.now(), TransferStatus.REQUESTED, null, staffId));
        transfers.updateStatus(transferId, TransferStatus.APPROVED, staffId);
        transfers.updateStatus(transferId, TransferStatus.IN_TRANSIT, staffId);
        transfers.updateStatus(transferId, TransferStatus.RECEIVED, staffId);
        var completed = transfers.findAll().stream().filter(value -> value.id() == transferId).findFirst().orElseThrow();
        assertEquals(TransferStatus.RECEIVED, completed.status());
        assertEquals(2, units.findAll().stream().filter(value -> value.bloodBankId() == destinationId
                && value.bloodGroup() == BloodGroup.O_NEGATIVE).mapToInt(BloodUnit::quantity).sum());
        assertNotNull(completed.transferDate());

        assertFalse(users.findByUsername(donorUsername).orElseThrow().passwordHash().equals(new String(donorPassword)));
        java.util.Arrays.fill(donorPassword, '\0');
    }

    @Test
    void recordsDonationAndRollsBackWhenBankCapacityWouldBeExceeded() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long staffId = users.createAdmin("donation_admin_" + suffix,
                PasswordUtil.hash("integration-admin-password".toCharArray())).id();
        DonorService donorService = new DonorService(donors, audit, eligibility);
        Donor eligibleDonor = donor("Donation " + suffix, "donation_" + suffix + "@example.org",
                uniquePhone(), BloodGroup.B_POSITIVE, "Hyderabad");
        long donorId = donorService.registerByStaff(eligibleDonor, staffId);

        BloodBankService banks = new BloodBankService(new BloodBankDAO());
        long bankId = banks.create(new BloodBank(0, "Donation Bank " + suffix, "2 Collection Road",
                "Hyderabad", "+1 555 700 4002", "donationbank_" + suffix + "@example.org",
                1, 0, true));
        DonationService donations = new DonationService(new DonationDAO(), donors, eligibility);
        donations.record(donorId, 1, bankId, staffId);
        Donor afterDonation = donors.findById(donorId).orElseThrow();
        assertEquals(1, afterDonation.totalDonations());
        assertEquals(LocalDate.now(), afterDonation.lastDonationDate());
        assertFalse(eligibility.checkEligibility(afterDonation, LocalDate.now()));
        assertEquals(1, units.findAll().stream().filter(value -> value.bloodBankId() == bankId)
                .mapToInt(BloodUnit::quantity).sum());

        BloodUnit overflow = new BloodUnit(0, BloodGroup.B_POSITIVE, LocalDate.now(),
                LocalDate.now().plusDays(35), 1, "Shelf B", bankId, BloodUnitStatus.AVAILABLE);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> inventory.add(overflow, staffId));
        assertEquals(1, units.findAll().stream().filter(value -> value.bloodBankId() == bankId)
                .mapToInt(BloodUnit::quantity).sum());
    }

    private Donor donor(String name, String email, String phone, BloodGroup group, String city) {
        LocalDate dob = LocalDate.now().minusYears(25);
        return DonorService.newDonor(name, 25, "Unspecified", group, phone, email,
                "1 Donor Road", city, dob);
    }

    private BloodBank bank(String name, String email) {
        return new BloodBank(0, name, "3 Blood Bank Road", "Hyderabad",
                "+1 555 700 5001", email, 100, 0, true);
    }

    private BloodUnit unit(BloodGroup group, int quantity, BloodBank bank) {
        return new BloodUnit(0, group, LocalDate.now(), LocalDate.now().plusDays(35),
                quantity, "Integration shelf", bank.id(), BloodUnitStatus.AVAILABLE);
    }

    private String uniquePhone() {
        long number = Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L);
        return "+1 555 " + String.format("%010d", number);
    }
}
