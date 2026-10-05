package com.bloodmanagement.dao;

import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.model.Hospital;
import com.bloodmanagement.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class HospitalDAO {
    public List<Hospital> findAll() {
        String sql = "SELECT id, name, address, city, phone, email, emergency_contact, active FROM hospitals ORDER BY name";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Hospital> hospitals = new ArrayList<>();
            while (result.next()) {
                hospitals.add(new Hospital(result.getLong("id"), result.getString("name"), result.getString("address"),
                        result.getString("city"), result.getString("phone"), result.getString("email"),
                        result.getString("emergency_contact"), result.getBoolean("active")));
            }
            return hospitals;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load hospitals.", exception);
        }
    }

    public long create(Hospital hospital) {
        String sql = "INSERT INTO hospitals (name, address, city, phone, email, emergency_contact) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, hospital.name());
            statement.setString(2, hospital.address());
            statement.setString(3, hospital.city());
            statement.setString(4, hospital.phone());
            statement.setString(5, hospital.email());
            statement.setString(6, hospital.emergencyContact());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the new hospital ID.");
                }
                return keys.getLong(1);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not register hospital.", exception);
        }
    }
}
