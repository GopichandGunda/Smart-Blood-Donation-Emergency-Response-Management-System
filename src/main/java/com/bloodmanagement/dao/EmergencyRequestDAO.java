package com.bloodmanagement.dao;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.EmergencyLevel;
import com.bloodmanagement.enums.RequestStatus;
import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.model.EmergencyRequest;
import com.bloodmanagement.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public final class EmergencyRequestDAO {
    public List<EmergencyRequest> findAll() {
        String sql = """
                SELECT id, hospital_id, patient_name, patient_id, blood_group_required, units_required,
                       emergency_level, request_date, required_date, required_time, hospital_location,
                       contact_number, request_status, assigned_donor_id, approved_by
                FROM emergency_requests
                ORDER BY FIELD(emergency_level, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'), required_date, request_date
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<EmergencyRequest> requests = new ArrayList<>();
            while (result.next()) {
                requests.add(map(result));
            }
            return requests;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load emergency requests.", exception);
        }
    }

    public long create(EmergencyRequest request, long userId) {
        String sql = """
                INSERT INTO emergency_requests (hospital_id, patient_name, patient_id, blood_group_required,
                    units_required, emergency_level, required_date, required_time, hospital_location,
                    contact_number, request_status, approved_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'UNDER_REVIEW', ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, request.hospitalId());
            statement.setString(2, request.patientName());
            statement.setString(3, request.patientId());
            statement.setString(4, request.bloodGroupRequired().getDisplayName());
            statement.setInt(5, request.unitsRequired());
            statement.setString(6, request.emergencyLevel().name());
            statement.setDate(7, Date.valueOf(request.requiredDate()));
            statement.setTime(8, Time.valueOf(request.requiredTime()));
            statement.setString(9, request.hospitalLocation());
            statement.setString(10, request.contactNumber());
            statement.setLong(11, userId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the new request ID.");
                }
                return keys.getLong(1);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create emergency request.", exception);
        }
    }

    public void updateStatus(long id, RequestStatus status) {
        if (status != RequestStatus.MATCHING && status != RequestStatus.UNDER_REVIEW
                && status != RequestStatus.CANCELLED && status != RequestStatus.EXPIRED) {
            throw new IllegalArgumentException("Use the matching or inventory workflow for this status.");
        }
        String sql = """
                UPDATE emergency_requests SET request_status = ?
                WHERE id = ? AND (
                    (? IN ('UNDER_REVIEW', 'MATCHING') AND request_status IN ('UNDER_REVIEW', 'MATCHING'))
                    OR (? IN ('CANCELLED', 'EXPIRED') AND request_status IN ('UNDER_REVIEW', 'MATCHING', 'DONOR_FOUND'))
                )
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, id);
            statement.setString(3, status.name());
            statement.setString(4, status.name());
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Request not found or its current status does not allow this transition.");
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update emergency request.", exception);
        }
    }

    private EmergencyRequest map(ResultSet result) throws SQLException {
        var assigned = result.getLong("assigned_donor_id");
        Long assignedDonorId = result.wasNull() ? null : assigned;
        var approved = result.getLong("approved_by");
        Long approvedBy = result.wasNull() ? null : approved;
        return new EmergencyRequest(result.getLong("id"), result.getLong("hospital_id"),
                result.getString("patient_name"), result.getString("patient_id"),
                BloodGroup.fromDisplayName(result.getString("blood_group_required")),
                result.getInt("units_required"), EmergencyLevel.valueOf(result.getString("emergency_level")),
                result.getTimestamp("request_date").toLocalDateTime().toLocalDate(),
                result.getDate("required_date").toLocalDate(), result.getTime("required_time").toLocalTime(),
                result.getString("hospital_location"), result.getString("contact_number"),
                RequestStatus.valueOf(result.getString("request_status")), assignedDonorId, approvedBy);
    }
}
