package com.bloodmanagement.service;

import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.dao.BloodUnitDAO;
import com.bloodmanagement.enums.BloodUnitStatus;
import com.bloodmanagement.exception.IncompatibleBloodGroupException;
import com.bloodmanagement.model.BloodUnit;
import com.bloodmanagement.util.ValidationUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class BloodInventoryService {
    private final BloodUnitDAO units;
    private final AuditLogDAO audit;
    private final InventoryAlertService alerts;
    private final BloodCompatibilityService compatibility;

    public BloodInventoryService(BloodUnitDAO units, AuditLogDAO audit, InventoryAlertService alerts,
            BloodCompatibilityService compatibility) {
        this.units = units;
        this.audit = audit;
        this.alerts = alerts;
        this.compatibility = compatibility;
    }

    public List<BloodUnit> findAll() {
        units.markExpired();
        return units.findAll();
    }

    public Map<String, Integer> stockByBloodGroup() {
        return units.stockByBloodGroup();
    }

    public List<InventoryAlertService.Alert> alerts() {
        return alerts.evaluate(stockByBloodGroup());
    }

    public long add(BloodUnit unit, long actorId) {
        ValidationUtil.positive(unit.quantity(), "Quantity");
        if (unit.bloodBankId() <= 0 || unit.collectionDate() == null || unit.expiryDate() == null
                || !unit.expiryDate().isAfter(unit.collectionDate()) || unit.expiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Provide a valid bank, collection date, and future expiry date.");
        }
        long id = units.add(unit);
        audit.record(actorId, "BLOOD_STOCK_ADDED", "Added " + unit.quantity() + " units of " + unit.bloodGroup());
        return id;
    }

    public int availableStock(com.bloodmanagement.enums.BloodGroup group) {
        return units.availableStock(group);
    }

    public void reserve(long requestId, com.bloodmanagement.enums.BloodGroup donorGroup,
            com.bloodmanagement.enums.BloodGroup requiredGroup, int quantity, long actorId) {
        ValidationUtil.positive(quantity, "Quantity");
        if (!compatibility.isCompatible(donorGroup, requiredGroup)) {
            throw new IncompatibleBloodGroupException(
                    donorGroup + " is not in the project's red-cell compatibility matrix for " + requiredGroup + ".");
        }
        units.reserve(requestId, donorGroup, quantity);
        audit.record(actorId, "BLOOD_RESERVED", quantity + " units reserved for request #" + requestId);
    }

    public void issue(long requestId, long actorId) {
        units.issueReservation(requestId);
        audit.record(actorId, "BLOOD_ISSUED", "Issued reservation for request #" + requestId);
    }

    public void cancelReservation(long requestId, long actorId) {
        units.cancelReservation(requestId);
        audit.record(actorId, "BLOOD_RESERVATION_CANCELLED", "Cancelled reservation for request #" + requestId);
    }

    public BloodUnitStatus status(long id) {
        return findAll().stream().filter(unit -> unit.id() == id).map(BloodUnit::status).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Blood unit not found."));
    }
}
