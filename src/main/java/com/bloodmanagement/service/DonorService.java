package com.bloodmanagement.service;

import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.enums.DonorStatus;
import com.bloodmanagement.exception.DuplicateRecordException;
import com.bloodmanagement.model.Donor;
import com.bloodmanagement.util.PasswordUtil;
import com.bloodmanagement.util.ValidationUtil;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

public final class DonorService {
    private final DonorDAO donors;
    private final AuditLogDAO audit;
    private final DonorEligibilityService eligibility;

    public DonorService(DonorDAO donors, AuditLogDAO audit, DonorEligibilityService eligibility) {
        this.donors = donors;
        this.audit = audit;
        this.eligibility = eligibility;
    }

    public List<Donor> findAll() {
        return donors.findAll().stream().map(this::withCurrentEligibility).toList();
    }

    public Donor findForUser(long userId) {
        return donors.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException(
                "No donor profile is linked to this account. Ask blood-bank staff to link your account."));
    }

    public List<Donor> search(String query) {
        return donors.search(query == null ? "" : query).stream().map(this::withCurrentEligibility).toList();
    }

    public long register(Donor draft, String username, char[] password, Long actorId) {
        validate(draft);
        String safeUsername = ValidationUtil.username(username);
        if (password == null || password.length < 12) {
            throw new IllegalArgumentException("Password must contain at least 12 characters.");
        }
        try {
            long id = donors.createWithUser(draft, safeUsername, PasswordUtil.hash(password));
            audit.record(actorId, "DONOR_REGISTERED", "Registered donor #" + id);
            return id;
        } catch (com.bloodmanagement.exception.DatabaseException exception) {
            if (exception.getCause() instanceof SQLException sql
                    && "23000".equals(sql.getSQLState())) {
                throw new DuplicateRecordException("Username, phone, or email is already registered.");
            }
            throw exception;
        }
    }

    public long registerByStaff(Donor draft, Long actorId) {
        validate(draft);
        try {
            long id = donors.create(draft);
            audit.record(actorId, "DONOR_REGISTERED", "Registered donor #" + id);
            return id;
        } catch (com.bloodmanagement.exception.DatabaseException exception) {
            if (exception.getCause() instanceof SQLException sql && "23000".equals(sql.getSQLState())) {
                throw new DuplicateRecordException("Phone or email is already registered.");
            }
            throw exception;
        }
    }

    public void setAvailability(long donorId, boolean available, Long actorId) {
        donors.updateAvailability(donorId, available);
        audit.record(actorId, "DONOR_AVAILABILITY_UPDATED", "Updated donor #" + donorId + " availability");
    }

    public void setStatus(long donorId, DonorStatus status, Long actorId) {
        donors.setStatus(donorId, status);
        audit.record(actorId, "DONOR_STATUS_UPDATED", "Updated donor #" + donorId + " account status");
    }

    private void validate(Donor donor) {
        ValidationUtil.required(donor.fullName(), "Name");
        ValidationUtil.age(donor.age());
        ValidationUtil.phone(donor.phone());
        ValidationUtil.email(donor.email());
        ValidationUtil.required(donor.address(), "Address");
        ValidationUtil.required(donor.city(), "City");
        if (donor.dateOfBirth() == null || donor.dateOfBirth().isAfter(LocalDate.now())
                || Period.between(donor.dateOfBirth(), LocalDate.now()).getYears() != donor.age()) {
            throw new IllegalArgumentException("Date of birth must be valid and match the donor's age.");
        }
    }

    private Donor withCurrentEligibility(Donor donor) {
        return new Donor(donor.id(), donor.fullName(), donor.age(), donor.gender(), donor.bloodGroup(),
                donor.phone(), donor.email(), donor.address(), donor.city(), donor.dateOfBirth(),
                donor.lastDonationDate(), donor.totalDonations(), donor.available(),
                eligibility.checkEligibility(donor, LocalDate.now()), donor.status(), donor.registrationDate());
    }

    public static Donor newDonor(String name, int age, String gender,
            com.bloodmanagement.enums.BloodGroup group, String phone, String email,
            String address, String city, LocalDate dateOfBirth) {
        return new Donor(0, name.trim(), age, gender, group, phone.trim(), email.trim(), address.trim(),
                city.trim(), dateOfBirth, null, 0, true, false, DonorStatus.ACTIVE, LocalDateTime.now());
    }
}
