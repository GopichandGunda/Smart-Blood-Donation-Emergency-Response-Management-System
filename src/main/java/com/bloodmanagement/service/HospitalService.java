package com.bloodmanagement.service;

import com.bloodmanagement.dao.HospitalDAO;
import com.bloodmanagement.model.Hospital;
import com.bloodmanagement.util.ValidationUtil;
import java.util.List;

public final class HospitalService {
    private final HospitalDAO hospitals;

    public HospitalService(HospitalDAO hospitals) {
        this.hospitals = hospitals;
    }

    public List<Hospital> findAll() {
        return hospitals.findAll();
    }

    public long create(Hospital hospital) {
        ValidationUtil.required(hospital.name(), "Hospital name");
        ValidationUtil.phone(hospital.phone());
        ValidationUtil.phone(hospital.emergencyContact());
        ValidationUtil.email(hospital.email());
        ValidationUtil.required(hospital.address(), "Address");
        ValidationUtil.required(hospital.city(), "City");
        return hospitals.create(hospital);
    }
}
