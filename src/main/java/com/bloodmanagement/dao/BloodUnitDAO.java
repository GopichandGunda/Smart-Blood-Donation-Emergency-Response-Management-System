package com.bloodmanagement.dao;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.BloodUnitStatus;
import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.exception.InsufficientBloodStockException;
import com.bloodmanagement.model.BloodUnit;
import com.bloodmanagement.util.DatabaseConnection;
import com.bloodmanagement.util.JdbcTransaction;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BloodUnitDAO {
    public List<BloodUnit> findAll() {
        String sql = """
                SELECT id, blood_group, collection_date, expiry_date, quantity, storage_location, blood_bank_id, status
                FROM blood_units WHERE quantity > 0 ORDER BY expiry_date, blood_group
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<BloodUnit> units = new ArrayList<>();
            while (result.next()) {
                units.add(map(result));
            }
            return units;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load blood inventory.", exception);
        }
    }

    public long add(BloodUnit unit) {
        return JdbcTransaction.execute("Blood stock intake", connection -> {
            verifyCapacity(connection, unit.bloodBankId(), unit.quantity());
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO blood_units
                        (blood_group, collection_date, expiry_date, quantity, storage_location, blood_bank_id, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE')
                    """, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, unit.bloodGroup().getDisplayName());
                statement.setDate(2, Date.valueOf(unit.collectionDate()));
                statement.setDate(3, Date.valueOf(unit.expiryDate()));
                statement.setInt(4, unit.quantity());
                statement.setString(5, unit.storageLocation());
                statement.setLong(6, unit.bloodBankId());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Database did not return the new blood unit ID.");
                    }
                    return keys.getLong(1);
                }
            }
        });
    }

    public Map<String, Integer> stockByBloodGroup() {
        String sql = """
                SELECT blood_group, SUM(quantity) AS total
                FROM blood_units
                WHERE status = 'AVAILABLE' AND expiry_date > CURRENT_DATE
                GROUP BY blood_group
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            Map<String, Integer> stock = new TreeMap<>();
            for (BloodGroup group : BloodGroup.values()) {
                stock.put(group.getDisplayName(), 0);
            }
            while (result.next()) {
                stock.put(result.getString("blood_group"), result.getInt("total"));
            }
            return stock;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not calculate blood stock.", exception);
        }
    }

    public int availableStock(BloodGroup group) {
        String sql = """
                SELECT COALESCE(SUM(quantity), 0) FROM blood_units
                WHERE blood_group = ? AND status = 'AVAILABLE' AND expiry_date > CURRENT_DATE
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, group.getDisplayName());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not check blood stock.", exception);
        }
    }

    public void reserve(long requestId, BloodGroup group, int units) {
        JdbcTransaction.execute("Blood reservation", connection -> {
            String select = """
                    SELECT id, collection_date, expiry_date, quantity, storage_location, blood_bank_id
                    FROM blood_units
                    WHERE blood_group = ? AND status = 'AVAILABLE' AND expiry_date > CURRENT_DATE AND quantity > 0
                    ORDER BY expiry_date, id
                    FOR UPDATE
                    """;
            List<BloodUnit> candidates = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setString(1, group.getDisplayName());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        candidates.add(new BloodUnit(result.getLong("id"), group,
                                result.getDate("collection_date").toLocalDate(),
                                result.getDate("expiry_date").toLocalDate(), result.getInt("quantity"),
                                result.getString("storage_location"), result.getLong("blood_bank_id"),
                                BloodUnitStatus.AVAILABLE));
                    }
                }
            }
            int available = candidates.stream().mapToInt(BloodUnit::quantity).sum();
            if (available < units) {
                throw new InsufficientBloodStockException(
                        "Insufficient " + group + " stock. Available: " + available + ", requested: " + units + ".");
            }
            int remaining = units;
            for (BloodUnit unit : candidates) {
                if (remaining == 0) {
                    break;
                }
                int reserved = Math.min(remaining, unit.quantity());
                if (reserved == unit.quantity()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE blood_units SET status = 'RESERVED', reserved_for_request_id = ? WHERE id = ?")) {
                        update.setLong(1, requestId);
                        update.setLong(2, unit.id());
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement reduce = connection.prepareStatement(
                            "UPDATE blood_units SET quantity = quantity - ? WHERE id = ?")) {
                        reduce.setInt(1, reserved);
                        reduce.setLong(2, unit.id());
                        reduce.executeUpdate();
                    }
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT INTO blood_units (blood_group, collection_date, expiry_date, quantity,
                                storage_location, blood_bank_id, status, reserved_for_request_id)
                            VALUES (?, ?, ?, ?, ?, ?, 'RESERVED', ?)
                            """)) {
                        insert.setString(1, group.getDisplayName());
                        insert.setDate(2, Date.valueOf(unit.collectionDate()));
                        insert.setDate(3, Date.valueOf(unit.expiryDate()));
                        insert.setInt(4, reserved);
                        insert.setString(5, unit.storageLocation());
                        insert.setLong(6, unit.bloodBankId());
                        insert.setLong(7, requestId);
                        insert.executeUpdate();
                    }
                }
                remaining -= reserved;
            }
            try (PreparedStatement updateRequest = connection.prepareStatement(
                    """
                    UPDATE emergency_requests SET request_status = 'BLOOD_RESERVED'
                    WHERE id = ? AND request_status IN ('UNDER_REVIEW', 'MATCHING', 'DONOR_FOUND')
                    """)) {
                updateRequest.setLong(1, requestId);
                if (updateRequest.executeUpdate() != 1) {
                    throw new SQLException("Request is not in a reservable state.");
                }
            }
            return null;
        });
    }

    public void cancelReservation(long requestId) {
        JdbcTransaction.execute("Reservation cancellation", connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE blood_units SET
                        status = CASE WHEN expiry_date <= CURRENT_DATE THEN 'EXPIRED' ELSE 'AVAILABLE' END,
                        reserved_for_request_id = NULL
                    WHERE reserved_for_request_id = ? AND status = 'RESERVED'
                    """)) {
                statement.setLong(1, requestId);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("No active reservation exists for this request.");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE emergency_requests SET request_status = 'CANCELLED'
                    WHERE id = ? AND request_status = 'BLOOD_RESERVED'
                    """)) {
                statement.setLong(1, requestId);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Request is not awaiting reservation cancellation.");
                }
            }
            return null;
        });
    }

    public void issueReservation(long requestId) {
        JdbcTransaction.execute("Blood issue", connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE blood_units SET status = 'ISSUED'
                    WHERE reserved_for_request_id = ? AND status = 'RESERVED' AND expiry_date > CURRENT_DATE
                    """)) {
                statement.setLong(1, requestId);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("No reservation exists for this request.");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE emergency_requests SET request_status = 'FULFILLED' WHERE id = ? AND request_status = 'BLOOD_RESERVED'")) {
                statement.setLong(1, requestId);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Request is not awaiting issue.");
                }
            }
            return null;
        });
    }

    public void markExpired() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE blood_units SET status = 'EXPIRED'
                     WHERE status = 'AVAILABLE' AND expiry_date <= CURRENT_DATE
                     """)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update expired stock.", exception);
        }
    }

    private BloodUnit map(ResultSet result) throws SQLException {
        return new BloodUnit(result.getLong("id"), BloodGroup.fromDisplayName(result.getString("blood_group")),
                result.getDate("collection_date").toLocalDate(), result.getDate("expiry_date").toLocalDate(),
                result.getInt("quantity"), result.getString("storage_location"), result.getLong("blood_bank_id"),
                BloodUnitStatus.valueOf(result.getString("status")));
    }

    static void verifyCapacity(Connection connection, long bankId, int additionalUnits) throws SQLException {
        int capacity;
        try (PreparedStatement bank = connection.prepareStatement(
                "SELECT storage_capacity FROM blood_banks WHERE id = ? AND active = TRUE FOR UPDATE")) {
            bank.setLong(1, bankId);
            try (ResultSet result = bank.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Blood bank not found or inactive.");
                }
                capacity = result.getInt(1);
            }
        }
        int occupied;
        try (PreparedStatement stock = connection.prepareStatement("""
                SELECT COALESCE(SUM(quantity), 0) FROM blood_units
                WHERE blood_bank_id = ? AND status IN ('AVAILABLE', 'RESERVED', 'EXPIRED')
                """)) {
            stock.setLong(1, bankId);
            try (ResultSet result = stock.executeQuery()) {
                result.next();
                occupied = result.getInt(1);
            }
        }
        if (occupied + additionalUnits > capacity) {
            throw new IllegalArgumentException("Blood bank storage capacity exceeded. Available capacity: "
                    + Math.max(0, capacity - occupied) + " units.");
        }
    }
}
