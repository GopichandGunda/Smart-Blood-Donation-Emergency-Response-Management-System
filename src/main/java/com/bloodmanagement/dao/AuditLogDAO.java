package com.bloodmanagement.dao;

import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.model.AuditLog;
import com.bloodmanagement.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class AuditLogDAO {
    public void record(Long userId, String action, String description) {
        String sql = "INSERT INTO audit_logs (user_id, action, description) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, userId);
            }
            statement.setString(2, action);
            statement.setString(3, description);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not record audit event.", exception);
        }
    }

    public List<AuditLog> findRecent() {
        String sql = """
                SELECT id, user_id, action, created_at, description
                FROM audit_logs ORDER BY created_at DESC, id DESC LIMIT 500
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             java.sql.ResultSet result = statement.executeQuery()) {
            List<AuditLog> logs = new ArrayList<>();
            while (result.next()) {
                long userId = result.getLong("user_id");
                Long nullableUserId = result.wasNull() ? null : userId;
                logs.add(new AuditLog(result.getLong("id"), nullableUserId, result.getString("action"),
                        result.getTimestamp("created_at").toLocalDateTime(), result.getString("description")));
            }
            return logs;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load audit logs.", exception);
        }
    }
}
