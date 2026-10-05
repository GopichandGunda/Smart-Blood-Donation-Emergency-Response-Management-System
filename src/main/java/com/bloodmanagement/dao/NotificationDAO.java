package com.bloodmanagement.dao;

import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.model.Notification;
import com.bloodmanagement.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class NotificationDAO {
    public List<Notification> findByUserId(long userId) {
        String sql = """
                SELECT id, user_id, message, created_at, is_read
                FROM notifications WHERE user_id = ? ORDER BY created_at DESC
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<Notification> notifications = new ArrayList<>();
                while (result.next()) {
                    notifications.add(new Notification(result.getLong("id"), result.getLong("user_id"),
                            result.getString("message"), result.getTimestamp("created_at").toLocalDateTime(),
                            result.getBoolean("is_read")));
                }
                return notifications;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load notifications.", exception);
        }
    }

    public void markRead(long id, long userId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?")) {
            statement.setLong(1, id);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update notification.", exception);
        }
    }
}
