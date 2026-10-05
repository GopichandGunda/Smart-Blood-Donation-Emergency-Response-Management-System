package com.bloodmanagement.service;

import com.bloodmanagement.dao.BloodTransferDAO;
import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.model.BloodTransfer;
import com.bloodmanagement.util.ValidationUtil;
import java.util.List;

public final class BloodTransferService {
    private final BloodTransferDAO transfers;
    private final AuditLogDAO audit;

    public BloodTransferService(BloodTransferDAO transfers, AuditLogDAO audit) {
        this.transfers = transfers;
        this.audit = audit;
    }

    public List<BloodTransfer> findAll() {
        return transfers.findAll();
    }

    public long create(BloodTransfer transfer) {
        ValidationUtil.positive(transfer.units(), "Transfer units");
        if (transfer.sourceBloodBankId() == transfer.destinationBloodBankId()) {
            throw new IllegalArgumentException("Source and destination blood banks must be different.");
        }
        long id = transfers.create(transfer);
        audit.record(transfer.staffId(), "BLOOD_TRANSFER_REQUESTED", "Requested transfer #" + id);
        return id;
    }

    public void updateStatus(long id, com.bloodmanagement.enums.TransferStatus status, long actorId) {
        transfers.updateStatus(id, status);
        audit.record(actorId, "BLOOD_TRANSFER_" + status.name(), "Transfer #" + id + " moved to " + status);
    }
}
