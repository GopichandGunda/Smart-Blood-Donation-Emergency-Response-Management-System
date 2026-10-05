package com.bloodmanagement.service;

import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.dao.BloodBankDAO;
import com.bloodmanagement.dao.BloodTransferDAO;
import com.bloodmanagement.dao.BloodUnitDAO;
import com.bloodmanagement.dao.DonationDAO;
import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.dao.DonorMatchDAO;
import com.bloodmanagement.dao.EmergencyRequestDAO;
import com.bloodmanagement.dao.HospitalDAO;
import com.bloodmanagement.dao.UserDAO;
import com.bloodmanagement.dao.NotificationDAO;

public final class AppServices {
    private final UserDAO users = new UserDAO();
    private final DonorDAO donors = new DonorDAO();
    private final EmergencyRequestDAO requests = new EmergencyRequestDAO();
    private final BloodUnitDAO units = new BloodUnitDAO();
    private final AuditLogDAO audit = new AuditLogDAO();
    private final BloodCompatibilityService compatibility = new BloodCompatibilityService();
    private final DonorEligibilityService eligibility = new DonorEligibilityService(donors);
    private final InventoryAlertService inventoryAlerts = new InventoryAlertService();

    public final AuthenticationService authentication = new AuthenticationService(users, audit);
    public final AccountService accounts = new AccountService(users, audit);
    public final DonorService donorService = new DonorService(donors, audit, eligibility);
    public final DonorEligibilityService donorEligibility = eligibility;
    public final DonorMatchingService matching = new DonorMatchingService(donors, compatibility, eligibility);
    public final DonorMatchService donorMatches = new DonorMatchService(new DonorMatchDAO(), audit);
    public final NotificationService notifications = new NotificationService(new NotificationDAO());
    public final EmergencyRequestService emergencyRequests = new EmergencyRequestService(requests, audit);
    public final BloodInventoryService inventory =
            new BloodInventoryService(units, audit, inventoryAlerts, compatibility);
    public final EmergencyProcessingService emergencyProcessing =
            new EmergencyProcessingService(matching, inventory);
    public final DonationService donations = new DonationService(new DonationDAO(), donors, eligibility);
    public final HospitalService hospitals = new HospitalService(new HospitalDAO());
    public final BloodBankService bloodBanks = new BloodBankService(new BloodBankDAO());
    public final BloodTransferService transfers = new BloodTransferService(new BloodTransferDAO(), audit);
    public final ReportService reports = new ReportService(donors, requests, units, users);
    public final AuditLogDAO auditLogs = audit;
    public final UserDAO userAccounts = users;
}
