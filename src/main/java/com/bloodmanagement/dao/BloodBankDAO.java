package com.bloodmanagement.dao;

import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.model.BloodBank;
import com.bloodmanagement.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BloodBankDAO {
    public List<BloodBank> findAll() {
        String sql = """
                SELECT b.id, b.name, b.address, b.city, b.phone, b.email, b.storage_capacity, b.active,
                       COALESCE(SUM(CASE WHEN u.status = 'AVAILABLE' AND u.expiry_date > CURRENT_DATE
                                         THEN u.quantity ELSE 0 END), 0) AS stock
                FROM blood_banks b LEFT JOIN blood_units u ON u.blood_bank_id = b.id
                GROUP BY b.id ORDER BY b.name
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<BloodBank> banks = new ArrayList<>();
            while (result.next()) {
                banks.add(new BloodBank(result.getLong("id"), result.getString("name"), result.getString("address"),
                        result.getString("city"), result.getString("phone"), result.getString("email"),
                        result.getInt("storage_capacity"), result.getInt("stock"), result.getBoolean("active")));
            }
            return banks;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load blood banks.", exception);
        }
    }

    public long create(BloodBank bank) {
        String sql = "INSERT INTO blood_banks (name, address, city, phone, email, storage_capacity) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, bank.name());
            statement.setString(2, bank.address());
            statement.setString(3, bank.city());
            statement.setString(4, bank.phone());
            statement.setString(5, bank.email());
            statement.setInt(6, bank.storageCapacity());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the new blood bank ID.");
                }
                return keys.getLong(1);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not register blood bank.", exception);
        }
    }
}
