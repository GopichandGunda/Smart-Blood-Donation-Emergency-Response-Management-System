package com.bloodmanagement.service;

import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.dao.EmergencyRequestDAO;
import com.bloodmanagement.enums.EmergencyLevel;
import com.bloodmanagement.enums.RequestStatus;
import com.bloodmanagement.model.EmergencyRequest;
import com.bloodmanagement.util.ValidationUtil;
import java.time.LocalDate;
import java.util.List;

public final class EmergencyRequestService {
    private final EmergencyRequestDAO requests;
    private final AuditLogDAO audit;

    public EmergencyRequestService(EmergencyRequestDAO requests, AuditLogDAO audit) {
        this.requests = requests;
        this.audit = audit;
    }

    public List<EmergencyRequest> findAll() {
        return requests.findAll();
    }

    public long create(EmergencyRequest request, long userId) {
        ValidationUtil.required(request.patientName(), "Patient name");
        ValidationUtil.required(request.patientId(), "Patient ID");
        ValidationUtil.positive(request.unitsRequired(), "Units required");
        ValidationUtil.phone(request.contactNumber());
        ValidationUtil.required(request.hospitalLocation(), "Hospital location");
        if (request.hospitalId() <= 0 || request.requiredDate() == null
                || request.requiredDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Choose a valid hospital and a required date today or later.");
        }
        if (request.emergencyLevel() == null) {
            throw new IllegalArgumentException("Choose an emergency priority.");
        }
        long id = requests.create(request, userId);
        audit.record(userId, "EMERGENCY_REQUEST_CREATED",
                request.emergencyLevel() + " request #" + id + " for " + request.bloodGroupRequired());
        return id;
    }

    public void updateStatus(long id, RequestStatus status, long actorId) {
        if (status == RequestStatus.FULFILLED || status == RequestStatus.BLOOD_RESERVED
                || status == RequestStatus.DONOR_FOUND) {
            throw new IllegalArgumentException("Use inventory issue/reservation actions to change this request status.");
        }
        requests.updateStatus(id, status);
        audit.record(actorId, "REQUEST_STATUS_UPDATED", "Request #" + id + " updated to " + status);
    }

    public boolean isCritical(EmergencyRequest request) {
        return request.emergencyLevel() == EmergencyLevel.CRITICAL;
    }
}
