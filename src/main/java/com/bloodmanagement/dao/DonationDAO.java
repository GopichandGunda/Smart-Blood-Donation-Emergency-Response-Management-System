package com.bloodmanagement.dao;

import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.exception.IneligibleDonorException;
import com.bloodmanagement.model.Donor;
import com.bloodmanagement.model.Donation;
import com.bloodmanagement.util.DatabaseConnection;
import com.bloodmanagement.util.JdbcTransaction;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class DonationDAO {
    public List<Donation> findForDonor(long donorId) {
        String sql = """
                SELECT id, donor_id, blood_group, donation_date, units, blood_bank_id, staff_id, verification_status
                FROM donations WHERE donor_id = ? ORDER BY donation_date DESC, id DESC
                """;
        try (var connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, donorId);
            try (ResultSet result = statement.executeQuery()) {
                List<Donation> history = new ArrayList<>();
                while (result.next()) {
                    history.add(new Donation(result.getLong("id"), result.getLong("donor_id"),
                            com.bloodmanagement.enums.BloodGroup.fromDisplayName(result.getString("blood_group")),
                            result.getDate("donation_date").toLocalDate(), result.getInt("units"),
                            result.getLong("blood_bank_id"), result.getLong("staff_id"),
                            "VERIFIED".equals(result.getString("verification_status"))));
                }
                return history;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load donation history.", exception);
        }
    }

    public void record(Donation donation) {
        JdbcTransaction.execute("Donation recording", connection -> {
            Donor donor;
            try (PreparedStatement select = connection.prepareStatement("""
                    SELECT id, full_name, age, gender, blood_group, phone, email, address, city,
                           date_of_birth, last_donation_date, total_donations, available, eligible,
                           account_status, registration_date
                    FROM donors WHERE id = ? FOR UPDATE
                    """)) {
                select.setLong(1, donation.donorId());
                try (ResultSet result = select.executeQuery()) {
                    if (!result.next()) {
                        throw new IneligibleDonorException("The donor account does not exist.");
                    }
                    Date lastDate = result.getDate("last_donation_date");
                    donor = new Donor(result.getLong("id"), result.getString("full_name"), result.getInt("age"),
                            result.getString("gender"),
                            com.bloodmanagement.enums.BloodGroup.fromDisplayName(result.getString("blood_group")),
                            result.getString("phone"), result.getString("email"), result.getString("address"),
                            result.getString("city"), result.getDate("date_of_birth").toLocalDate(),
                            lastDate == null ? null : lastDate.toLocalDate(), result.getInt("total_donations"),
                            result.getBoolean("available"), result.getBoolean("eligible"),
                            com.bloodmanagement.enums.DonorStatus.valueOf(result.getString("account_status")),
                            result.getTimestamp("registration_date").toLocalDateTime());
                }
            }
            if (!donor.available() || donor.status() != com.bloodmanagement.enums.DonorStatus.ACTIVE
                    || donor.age() < 18
                    || (donor.lastDonationDate() != null
                    && donor.lastDonationDate().plusDays(90).isAfter(donation.donationDate()))) {
                throw new IneligibleDonorException("Donor is not eligible for donation today.");
            }
            if (donation.donationDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Donation date cannot be in the future.");
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO donations (donor_id, blood_group, donation_date, units, blood_bank_id, staff_id, verification_status)
                    VALUES (?, ?, ?, ?, ?, ?, 'VERIFIED')
                    """)) {
                insert.setLong(1, donor.id());
                insert.setString(2, donor.bloodGroup().getDisplayName());
                insert.setDate(3, Date.valueOf(donation.donationDate()));
                insert.setInt(4, donation.units());
                insert.setLong(5, donation.bloodBankId());
                insert.setLong(6, donation.staffId());
                insert.executeUpdate();
            }
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE donors SET last_donation_date = ?, total_donations = total_donations + 1, eligible = FALSE
                    WHERE id = ?
                    """)) {
                update.setDate(1, Date.valueOf(donation.donationDate()));
                update.setLong(2, donor.id());
                update.executeUpdate();
            }
            try (PreparedStatement stock = connection.prepareStatement("""
                    INSERT INTO blood_units (blood_group, collection_date, expiry_date, quantity,
                        storage_location, blood_bank_id, status)
                    VALUES (?, ?, ?, ?, 'Newly collected', ?, 'AVAILABLE')
                    """)) {
                BloodUnitDAO.verifyCapacity(connection, donation.bloodBankId(), donation.units());
                stock.setString(1, donor.bloodGroup().getDisplayName());
                stock.setDate(2, Date.valueOf(donation.donationDate()));
                stock.setDate(3, Date.valueOf(donation.donationDate().plusDays(35)));
                stock.setInt(4, donation.units());
                stock.setLong(5, donation.bloodBankId());
                stock.executeUpdate();
            }
            try (PreparedStatement audit = connection.prepareStatement(
                    "INSERT INTO audit_logs (user_id, action, description) VALUES (?, 'DONATION_RECORDED', ?)")) {
                audit.setLong(1, donation.staffId());
                audit.setString(2, "Recorded " + donation.units() + " unit(s) from donor #" + donor.id());
                audit.executeUpdate();
            }
            return null;
        });
    }
}
