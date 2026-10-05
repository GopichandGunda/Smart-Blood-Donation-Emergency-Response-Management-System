package com.bloodmanagement.dao;

import com.bloodmanagement.enums.UserRole;
import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.model.User;
import com.bloodmanagement.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

public final class UserDAO {
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role, active, created_at FROM users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load account.", exception);
        }
    }

    public User createAdmin(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, 'ADMIN')";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the new account ID.");
                }
                return new User(keys.getLong(1), username, passwordHash, UserRole.ADMIN, true, LocalDateTime.now());
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create administrator account.", exception);
        }
    }

    public long createStaffAccount(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, 'BLOOD_BANK_STAFF')";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the new account ID.");
                }
                return keys.getLong(1);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create staff account.", exception);
        }
    }

    public java.util.List<User> findAll() {
        String sql = "SELECT id, username, password_hash, role, active, created_at FROM users ORDER BY username";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            java.util.List<User> users = new java.util.ArrayList<>();
            while (result.next()) {
                users.add(map(result));
            }
            return users;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load user accounts.", exception);
        }
    }

    public void setActive(long id, boolean active) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET active = ? WHERE id = ?")) {
            statement.setBoolean(1, active);
            statement.setLong(2, id);
            if (statement.executeUpdate() != 1) {
                throw new DatabaseException("Account not found.", new SQLException("No user row updated."));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update account status.", exception);
        }
    }

    private User map(ResultSet result) throws SQLException {
        return new User(result.getLong("id"), result.getString("username"), result.getString("password_hash"),
                UserRole.valueOf(result.getString("role")), result.getBoolean("active"),
                result.getTimestamp("created_at").toLocalDateTime());
    }
}
