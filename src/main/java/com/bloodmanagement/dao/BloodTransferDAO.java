package com.bloodmanagement.dao;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.TransferStatus;
import com.bloodmanagement.exception.DatabaseException;
import com.bloodmanagement.exception.InsufficientBloodStockException;
import com.bloodmanagement.model.BloodTransfer;
import com.bloodmanagement.model.BloodUnit;
import com.bloodmanagement.util.DatabaseConnection;
import com.bloodmanagement.util.JdbcTransaction;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class BloodTransferDAO {
    public List<BloodTransfer> findAll() {
        String sql = """
                SELECT id, source_blood_bank_id, destination_blood_bank_id, blood_group, units, request_date,
                       approval_status, transfer_date, staff_id FROM blood_transfers ORDER BY request_date DESC, id DESC
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<BloodTransfer> transfers = new ArrayList<>();
            while (result.next()) {
                Date date = result.getDate("transfer_date");
                transfers.add(new BloodTransfer(result.getLong("id"), result.getLong("source_blood_bank_id"),
                        result.getLong("destination_blood_bank_id"),
                        BloodGroup.fromDisplayName(result.getString("blood_group")), result.getInt("units"),
                        result.getDate("request_date").toLocalDate(),
                        TransferStatus.valueOf(result.getString("approval_status")),
                        date == null ? null : date.toLocalDate(), result.getLong("staff_id")));
            }
            return transfers;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load blood transfers.", exception);
        }
    }

    public long create(BloodTransfer transfer) {
        String sql = """
                INSERT INTO blood_transfers (source_blood_bank_id, destination_blood_bank_id, blood_group,
                    units, request_date, approval_status, staff_id)
                VALUES (?, ?, ?, ?, ?, 'REQUESTED', ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, transfer.sourceBloodBankId());
            statement.setLong(2, transfer.destinationBloodBankId());
            statement.setString(3, transfer.bloodGroup().getDisplayName());
            statement.setInt(4, transfer.units());
            statement.setDate(5, Date.valueOf(transfer.requestDate()));
            statement.setLong(6, transfer.staffId());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return the transfer ID.");
                }
                return keys.getLong(1);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create blood transfer request.", exception);
        }
    }

    public void updateStatus(long id, TransferStatus status) {
        switch (status) {
            case APPROVED -> simpleTransition(id, TransferStatus.REQUESTED, TransferStatus.APPROVED);
            case IN_TRANSIT -> dispatch(id);
            case RECEIVED -> receive(id);
            case CANCELLED -> cancel(id);
            case REQUESTED -> throw new IllegalArgumentException("Transfers cannot be moved back to REQUESTED.");
        }
    }

    private void simpleTransition(long id, TransferStatus expected, TransferStatus target) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE blood_transfers SET approval_status = ? WHERE id = ? AND approval_status = ?")) {
            statement.setString(1, target.name());
            statement.setLong(2, id);
            statement.setString(3, expected.name());
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Transfer not found or is not awaiting approval.");
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not approve transfer.", exception);
        }
    }

    private void dispatch(long id) {
        JdbcTransaction.execute("Blood transfer dispatch", connection -> {
            BloodTransfer transfer = lockedTransfer(connection, id);
            if (transfer.status() != TransferStatus.APPROVED) {
                throw new IllegalArgumentException("Only approved transfers can be dispatched.");
            }
            List<BloodUnit> stock = new ArrayList<>();
            String select = """
                    SELECT id, collection_date, expiry_date, quantity, storage_location
                    FROM blood_units WHERE blood_bank_id = ? AND blood_group = ?
                      AND status = 'AVAILABLE' AND expiry_date > CURRENT_DATE AND quantity > 0
                    ORDER BY expiry_date, id FOR UPDATE
                    """;
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setLong(1, transfer.sourceBloodBankId());
                statement.setString(2, transfer.bloodGroup().getDisplayName());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        stock.add(new BloodUnit(result.getLong("id"), transfer.bloodGroup(),
                                result.getDate("collection_date").toLocalDate(),
                                result.getDate("expiry_date").toLocalDate(), result.getInt("quantity"),
                                result.getString("storage_location"), transfer.sourceBloodBankId(),
                                com.bloodmanagement.enums.BloodUnitStatus.AVAILABLE));
                    }
                }
            }
            int total = stock.stream().mapToInt(BloodUnit::quantity).sum();
            if (total < transfer.units()) {
                throw new InsufficientBloodStockException("The source bank does not have enough unexpired available stock.");
            }
            int remaining = transfer.units();
            for (BloodUnit unit : stock) {
                if (remaining == 0) {
                    break;
                }
                int moved = Math.min(remaining, unit.quantity());
                if (moved == unit.quantity()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE blood_units SET status = 'ISSUED' WHERE id = ?")) {
                        update.setLong(1, unit.id());
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE blood_units SET quantity = quantity - ? WHERE id = ?")) {
                        update.setInt(1, moved);
                        update.setLong(2, unit.id());
                        update.executeUpdate();
                    }
                }
                try (PreparedStatement item = connection.prepareStatement("""
                        INSERT INTO blood_transfer_items
                            (transfer_id, blood_group, quantity, collection_date, expiry_date, storage_location)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)) {
                    item.setLong(1, transfer.id());
                    item.setString(2, transfer.bloodGroup().getDisplayName());
                    item.setInt(3, moved);
                    item.setDate(4, Date.valueOf(unit.collectionDate()));
                    item.setDate(5, Date.valueOf(unit.expiryDate()));
                    item.setString(6, unit.storageLocation());
                    item.executeUpdate();
                }
                remaining -= moved;
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE blood_transfers SET approval_status = 'IN_TRANSIT' WHERE id = ?")) {
                update.setLong(1, id);
                update.executeUpdate();
            }
            return null;
        });
    }

    private void receive(long id) {
        JdbcTransaction.execute("Blood transfer receipt", connection -> {
            BloodTransfer transfer = lockedTransfer(connection, id);
            if (transfer.status() != TransferStatus.IN_TRANSIT) {
                throw new IllegalArgumentException("Only in-transit transfers can be received.");
            }
            BloodUnitDAO.verifyCapacity(connection, transfer.destinationBloodBankId(), transfer.units());
            try (PreparedStatement items = connection.prepareStatement("""
                    SELECT blood_group, quantity, collection_date, expiry_date, storage_location
                    FROM blood_transfer_items WHERE transfer_id = ?
                    """)) {
                items.setLong(1, id);
                try (ResultSet result = items.executeQuery();
                     PreparedStatement stock = connection.prepareStatement("""
                             INSERT INTO blood_units (blood_group, collection_date, expiry_date, quantity,
                                 storage_location, blood_bank_id, status)
                             VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE')
                             """)) {
                    while (result.next()) {
                        stock.setString(1, result.getString("blood_group"));
                        stock.setDate(2, result.getDate("collection_date"));
                        stock.setDate(3, result.getDate("expiry_date"));
                        stock.setInt(4, result.getInt("quantity"));
                        stock.setString(5, "Transfer from bank #" + transfer.sourceBloodBankId()
                                + " / " + result.getString("storage_location"));
                        stock.setLong(6, transfer.destinationBloodBankId());
                        stock.addBatch();
                    }
                    if (stock.executeBatch().length == 0) {
                        throw new SQLException("Transfer has no recorded stock items.");
                    }
                }
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE blood_transfers SET approval_status = 'RECEIVED', transfer_date = CURRENT_DATE WHERE id = ?")) {
                update.setLong(1, id);
                update.executeUpdate();
            }
            return null;
        });
    }

    private void cancel(long id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE blood_transfers SET approval_status = 'CANCELLED'
                     WHERE id = ? AND approval_status IN ('REQUESTED', 'APPROVED')
                     """)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Only requested or approved transfers can be cancelled.");
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not cancel transfer.", exception);
        }
    }

    private BloodTransfer lockedTransfer(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, source_blood_bank_id, destination_blood_bank_id, blood_group, units,
                       request_date, approval_status, transfer_date, staff_id
                FROM blood_transfers WHERE id = ? FOR UPDATE
                """)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Blood transfer not found.");
                }
                Date date = result.getDate("transfer_date");
                return new BloodTransfer(result.getLong("id"), result.getLong("source_blood_bank_id"),
                        result.getLong("destination_blood_bank_id"),
                        BloodGroup.fromDisplayName(result.getString("blood_group")), result.getInt("units"),
                        result.getDate("request_date").toLocalDate(),
                        TransferStatus.valueOf(result.getString("approval_status")),
                        date == null ? null : date.toLocalDate(), result.getLong("staff_id"));
            }
        }
    }
}
