package com.bloodmanagement.dao;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.DonorStatus;
import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.model.Donor;
import com.bloodmanagement.util.DatabaseConnection;
import com.bloodmanagement.util.JdbcTransaction;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DonorDAO {
    private static final String SELECT = """
            SELECT id, full_name, age, gender, blood_group, phone, email, address, city,
                   date_of_birth, last_donation_date, total_donations, available, eligible,
                   account_status, registration_date
            FROM donors
            """;

    public List<Donor> findAll() {
        return query(SELECT + " ORDER BY full_name", null);
    }

    public List<Donor> search(String query) {
        String sql = SELECT + """
                WHERE full_name LIKE ? OR email LIKE ? OR phone LIKE ? OR city LIKE ? OR blood_group = ?
                ORDER BY full_name
                """;
        String term = "%" + query.trim() + "%";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 4; index++) {
                statement.setString(index, term);
            }
            statement.setString(5, query.trim().toUpperCase());
            try (ResultSet result = statement.executeQuery()) {
                return mapAll(result);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not search donors.", exception);
        }
    }

    public Optional<Donor> findById(long id) {
        List<Donor> donors = query(SELECT + " WHERE id = ?", id);
        return donors.stream().findFirst();
    }

    public Optional<Donor> findByUserId(long userId) {
        String sql = SELECT + " WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load donor profile.", exception);
        }
    }

    public long create(Donor donor) {
        return JdbcTransaction.execute("Donor registration", connection -> insert(connection, donor, null));
    }

    public long createWithUser(Donor donor, String username, String passwordHash) {
        return JdbcTransaction.execute("Donor registration", connection -> {
            long userId;
            try (PreparedStatement user = connection.prepareStatement(
                    "INSERT INTO users (username, password_hash, role) VALUES (?, ?, 'DONOR')",
                    Statement.RETURN_GENERATED_KEYS)) {
                user.setString(1, username);
                user.setString(2, passwordHash);
                user.executeUpdate();
                try (ResultSet keys = user.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Database did not return the new donor account ID.");
                    }
                    userId = keys.getLong(1);
                }
            }
            return insert(connection, donor, userId);
        });
    }

    public void updateAvailability(long donorId, boolean available) {
        String sql = "UPDATE donors SET available = ? WHERE id = ? AND account_status = 'ACTIVE'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, available);
            statement.setLong(2, donorId);
            if (statement.executeUpdate() != 1) {
                throw new DatabaseException("Donor was not found or is inactive.", new SQLException("No donor row updated."));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update donor availability.", exception);
        }
    }

    public void setStatus(long donorId, DonorStatus status) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE donors SET account_status = ?, available = CASE WHEN ? = 'ACTIVE' THEN available ELSE FALSE END
                     WHERE id = ?
                     """)) {
            statement.setString(1, status.name());
            statement.setString(2, status.name());
            statement.setLong(3, donorId);
            if (statement.executeUpdate() != 1) {
                throw new DatabaseException("Donor not found.", new SQLException("No donor row updated."));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update donor status.", exception);
        }
    }

    public void updateEligibility(long donorId, boolean eligible) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE donors SET eligible = ? WHERE id = ?")) {
            statement.setBoolean(1, eligible);
            statement.setLong(2, donorId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update donor eligibility.", exception);
        }
    }

    private long insert(Connection connection, Donor donor, Long userId) throws SQLException {
        String sql = """
                INSERT INTO donors (user_id, full_name, age, gender, blood_group, phone, email, address, city,
                    date_of_birth, last_donation_date, total_donations, available, eligible, account_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (userId == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, userId);
            }
            statement.setString(2, donor.fullName());
            statement.setInt(3, donor.age());
            statement.setString(4, donor.gender());
            statement.setString(5, donor.bloodGroup().getDisplayName());
            statement.setString(6, donor.phone());
            statement.setString(7, donor.email());
            statement.setString(8, donor.address());
            statement.setString(9, donor.city());
            statement.setDate(10, Date.valueOf(donor.dateOfBirth()));
            if (donor.lastDonationDate() == null) {
                statement.setNull(11, Types.DATE);
            } else {
                statement.setDate(11, Date.valueOf(donor.lastDonationDate()));
            }
            statement.setInt(12, donor.totalDonations());
            statement.setBoolean(13, donor.available());
            statement.setBoolean(14, donor.eligible());
            statement.setString(15, donor.status().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the new donor ID.");
                }
                return keys.getLong(1);
            }
        }
    }

    private List<Donor> query(String sql, Long id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (id != null) {
                statement.setLong(1, id);
            }
            try (ResultSet result = statement.executeQuery()) {
                return mapAll(result);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load donors.", exception);
        }
    }

    private List<Donor> mapAll(ResultSet result) throws SQLException {
        List<Donor> donors = new ArrayList<>();
        while (result.next()) {
            donors.add(map(result));
        }
        return donors;
    }

    private Donor map(ResultSet result) throws SQLException {
        Date lastDonation = result.getDate("last_donation_date");
        return new Donor(result.getLong("id"), result.getString("full_name"), result.getInt("age"),
                result.getString("gender"), BloodGroup.fromDisplayName(result.getString("blood_group")),
                result.getString("phone"), result.getString("email"), result.getString("address"), result.getString("city"),
                result.getDate("date_of_birth").toLocalDate(),
                lastDonation == null ? null : lastDonation.toLocalDate(),
                result.getInt("total_donations"), result.getBoolean("available"), result.getBoolean("eligible"),
                DonorStatus.valueOf(result.getString("account_status")),
                result.getTimestamp("registration_date").toLocalDateTime());
    }
}
