package com.bloodmanagement.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final String URL = setting("BLOOD_DB_URL",
            "jdbc:mysql://localhost:3306/emergency_blood_management?useSSL=true&serverTimezone=UTC");

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String user = System.getenv("BLOOD_DB_USER");
        String password = System.getenv("BLOOD_DB_PASSWORD");
        if (user == null || user.isBlank() || password == null || password.isBlank()) {
            throw new SQLException(
                    "Set BLOOD_DB_USER and BLOOD_DB_PASSWORD environment variables before starting the application.");
        }
        return DriverManager.getConnection(URL, user, password);
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
