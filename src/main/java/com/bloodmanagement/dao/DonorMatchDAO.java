package com.bloodmanagement.dao;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.EmergencyLevel;
import com.bloodmanagement.enums.RequestStatus;
import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.model.DonorMatch;
import com.bloodmanagement.service.DonorMatchingService.MatchResult;
import com.bloodmanagement.util.DatabaseConnection;
import com.bloodmanagement.util.JdbcTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class DonorMatchDAO {
    public void saveMatches(long requestId, List<MatchResult> matches) {
        JdbcTransaction.execute("Donor matching", connection -> {
            String insertMatch = """
                    INSERT INTO donor_matches (emergency_request_id, donor_id, matching_score)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE matching_score = VALUES(matching_score), matched_at = CURRENT_TIMESTAMP
                    """;
            try (PreparedStatement match = connection.prepareStatement(insertMatch);
                 PreparedStatement existing = connection.prepareStatement(
                         "SELECT 1 FROM donor_matches WHERE emergency_request_id = ? AND donor_id = ?");
                 PreparedStatement notice = connection.prepareStatement("""
                         INSERT INTO notifications (user_id, message)
                         SELECT user_id, ? FROM donors WHERE id = ? AND user_id IS NOT NULL
                         """)) {
                for (MatchResult candidate : matches) {
                    existing.setLong(1, requestId);
                    existing.setLong(2, candidate.donor().id());
                    boolean newMatch;
                    try (ResultSet result = existing.executeQuery()) {
                        newMatch = !result.next();
                    }
                    match.setLong(1, requestId);
                    match.setLong(2, candidate.donor().id());
                    match.setInt(3, candidate.score());
                    match.addBatch();
                    if (newMatch) {
                        notice.setString(1, "A compatible emergency blood donation opportunity is available. "
                                + "Review the offer in your donor account.");
                        notice.setLong(2, candidate.donor().id());
                        notice.addBatch();
                    }
                }
                match.executeBatch();
                notice.executeBatch();
            }
            return null;
        });
    }

    public List<Offer> findOffers(long userId) {
        String sql = """
                SELECT m.id AS match_id, m.emergency_request_id, m.donor_id, m.matching_score, m.matched_at, m.accepted,
                       r.blood_group_required, r.units_required, r.emergency_level, r.required_date,
                       r.hospital_location, r.request_status
                FROM donor_matches m
                JOIN donors d ON d.id = m.donor_id
                JOIN emergency_requests r ON r.id = m.emergency_request_id
                WHERE d.user_id = ?
                ORDER BY FIELD(r.emergency_level, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'),
                         m.matching_score DESC, m.matched_at DESC
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<Offer> offers = new ArrayList<>();
                while (result.next()) {
                    boolean accepted = result.getBoolean("accepted");
                    Boolean response = result.wasNull() ? null : accepted;
                    DonorMatch match = new DonorMatch(result.getLong("match_id"),
                            result.getLong("emergency_request_id"), result.getLong("donor_id"),
                            result.getInt("matching_score"), result.getTimestamp("matched_at").toLocalDateTime(),
                            Boolean.TRUE.equals(response));
                    offers.add(new Offer(match, BloodGroup.fromDisplayName(result.getString("blood_group_required")),
                            result.getInt("units_required"), EmergencyLevel.valueOf(result.getString("emergency_level")),
                            result.getDate("required_date").toLocalDate(), result.getString("hospital_location"),
                            RequestStatus.valueOf(result.getString("request_status")), response));
                }
                return offers;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load donor offers.", exception);
        }
    }

    public void respond(long matchId, long userId, boolean accepted) {
        JdbcTransaction.execute("Donor match response", connection -> {
            long requestId;
            long donorId;
            long assignedDonorId;
            RequestStatus requestStatus;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT m.emergency_request_id, m.donor_id, m.accepted, r.request_status,
                           r.assigned_donor_id
                    FROM donor_matches m
                    JOIN donors d ON d.id = m.donor_id
                    JOIN emergency_requests r ON r.id = m.emergency_request_id
                    WHERE m.id = ? AND d.user_id = ?
                    FOR UPDATE
                    """)) {
                statement.setLong(1, matchId);
                statement.setLong(2, userId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("Offer not found for this donor account.");
                    }
                    if (result.getObject("accepted") != null) {
                        throw new IllegalArgumentException("You have already responded to this offer.");
                    }
                    requestId = result.getLong("emergency_request_id");
                    donorId = result.getLong("donor_id");
                    assignedDonorId = result.getLong("assigned_donor_id");
                    if (result.wasNull()) {
                        assignedDonorId = 0;
                    }
                    requestStatus = RequestStatus.valueOf(result.getString("request_status"));
                }
            }
            if (requestStatus != RequestStatus.MATCHING && requestStatus != RequestStatus.DONOR_FOUND) {
                throw new IllegalArgumentException("This emergency request is no longer accepting donor responses.");
            }
            if (accepted) {
                if (assignedDonorId != 0 && assignedDonorId != donorId) {
                    throw new IllegalArgumentException("Another donor has already accepted this request.");
                }
                try (PreparedStatement donor = connection.prepareStatement("""
                        UPDATE donors SET available = FALSE
                        WHERE id = ? AND available = TRUE AND account_status = 'ACTIVE'
                        """)) {
                    donor.setLong(1, donorId);
                    if (donor.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Donor is no longer available for this request.");
                    }
                }
                try (PreparedStatement request = connection.prepareStatement("""
                        UPDATE emergency_requests SET request_status = 'DONOR_FOUND', assigned_donor_id = ?
                        WHERE id = ? AND request_status IN ('MATCHING', 'DONOR_FOUND')
                        AND (assigned_donor_id IS NULL OR assigned_donor_id = ?)
                        """)) {
                    request.setLong(1, donorId);
                    request.setLong(2, requestId);
                    request.setLong(3, donorId);
                    if (request.executeUpdate() != 1) {
                        throw new SQLException("Request has already been assigned to another donor.");
                    }
                }
            }
            try (PreparedStatement response = connection.prepareStatement(
                    "UPDATE donor_matches SET accepted = ? WHERE id = ? AND accepted IS NULL")) {
                response.setBoolean(1, accepted);
                response.setLong(2, matchId);
                if (response.executeUpdate() != 1) {
                    throw new SQLException("Offer response could not be recorded.");
                }
            }
            try (PreparedStatement notification = connection.prepareStatement(
                    "INSERT INTO notifications (user_id, message) VALUES (?, ?)")) {
                notification.setLong(1, userId);
                notification.setString(2, accepted
                        ? "Your acceptance for emergency request #" + requestId + " was recorded."
                        : "Your response for emergency request #" + requestId + " was recorded.");
                notification.executeUpdate();
            }
            return null;
        });
    }

    public record Offer(DonorMatch match, BloodGroup bloodGroup, int units, EmergencyLevel emergencyLevel,
            java.time.LocalDate requiredDate, String location, RequestStatus status, Boolean response) {
    }
}
