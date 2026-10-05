package com.bloodmanagement.service;

import com.bloodmanagement.dao.BloodBankDAO;
import com.bloodmanagement.model.BloodBank;
import com.bloodmanagement.util.ValidationUtil;
import java.util.List;

public final class BloodBankService {
    private final BloodBankDAO bloodBanks;

    public BloodBankService(BloodBankDAO bloodBanks) {
        this.bloodBanks = bloodBanks;
    }

    public List<BloodBank> findAll() {
        return bloodBanks.findAll();
    }

    public long create(BloodBank bank) {
        ValidationUtil.required(bank.name(), "Blood bank name");
        ValidationUtil.phone(bank.phone());
        ValidationUtil.email(bank.email());
        ValidationUtil.required(bank.address(), "Address");
        ValidationUtil.required(bank.city(), "City");
        ValidationUtil.positive(bank.storageCapacity(), "Storage capacity");
        return bloodBanks.create(bank);
    }
}
