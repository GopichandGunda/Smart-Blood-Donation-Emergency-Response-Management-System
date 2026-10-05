package com.bloodmanagement.service;

import com.bloodmanagement.dao.BloodUnitDAO;
import com.bloodmanagement.dao.DonorDAO;
import com.bloodmanagement.dao.EmergencyRequestDAO;
import com.bloodmanagement.dao.UserDAO;
import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.model.Donor;
import com.bloodmanagement.util.DatabaseConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReportService {
    private final DonorDAO donors;
    private final EmergencyRequestDAO requests;
    private final BloodUnitDAO units;
    private final UserDAO users;

    public ReportService(DonorDAO donors, EmergencyRequestDAO requests, BloodUnitDAO units, UserDAO users) {
        this.donors = donors;
        this.requests = requests;
        this.units = units;
        this.users = users;
    }

    public Map<String, Long> summary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("Total donors", (long) donors.findAll().size());
        summary.put("Active donors", donors.findAll().stream()
                .filter(donor -> donor.status() == com.bloodmanagement.enums.DonorStatus.ACTIVE).count());
        summary.put("Eligible donors", donors.findAll().stream().filter(Donor::eligible).count());
        summary.put("Emergency requests", (long) requests.findAll().size());
        summary.put("Pending requests", requests.findAll().stream()
                .filter(request -> request.status() != com.bloodmanagement.enums.RequestStatus.FULFILLED
                        && request.status() != com.bloodmanagement.enums.RequestStatus.CANCELLED
                        && request.status() != com.bloodmanagement.enums.RequestStatus.EXPIRED).count());
        summary.put("Critical requests", requests.findAll().stream()
                .filter(request -> request.emergencyLevel() == com.bloodmanagement.enums.EmergencyLevel.CRITICAL
                        && request.status() != com.bloodmanagement.enums.RequestStatus.FULFILLED
                        && request.status() != com.bloodmanagement.enums.RequestStatus.CANCELLED
                        && request.status() != com.bloodmanagement.enums.RequestStatus.EXPIRED).count());
        summary.put("Total donations", scalar("SELECT COUNT(*) FROM donations"));
        summary.put("Registered hospitals", scalar("SELECT COUNT(*) FROM hospitals"));
        summary.put("Active accounts", users.findAll().stream().filter(user -> user.active()).count());
        units.stockByBloodGroup().forEach((group, stock) -> summary.put("Available " + group + " units", (long) stock));
        summary.putAll(requestStatistics());
        return summary;
    }

    public void exportSummary(Path path) {
        String extension = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        StringBuilder output = new StringBuilder();
        if (extension.endsWith(".csv")) {
            output.append("Metric,Value").append(System.lineSeparator());
            summary().forEach((name, value) -> output.append(csv(name)).append(',').append(value)
                    .append(System.lineSeparator()));
        } else {
            output.append("Emergency Blood Management - System Report").append(System.lineSeparator())
                    .append("Generated: ").append(java.time.LocalDateTime.now()).append(System.lineSeparator())
                    .append(System.lineSeparator());
            summary().forEach((name, value) -> output.append(name).append(": ").append(value)
                    .append(System.lineSeparator()));
        }
        try {
            Files.writeString(path, output, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not export the report to " + path + ".", exception);
        }
    }

    private Map<String, Long> requestStatistics() {
        String sql = """
                SELECT request_status, COUNT(*) AS total FROM emergency_requests GROUP BY request_status
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            Map<String, Long> counts = new LinkedHashMap<>();
            while (result.next()) {
                counts.put(result.getString("request_status") + " requests", result.getLong("total"));
            }
            return counts;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not prepare request report.", exception);
        }
    }

    private long scalar(String sql) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not prepare system report.", exception);
        }
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
