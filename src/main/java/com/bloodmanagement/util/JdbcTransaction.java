package com.bloodmanagement.util;

import com.bloodmanagement.exception.DatabaseException;
import java.sql.Connection;
import java.sql.SQLException;

public final class JdbcTransaction {
    private JdbcTransaction() {
    }

    public static <T> T execute(String operation, Work<T> work) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                if (exception instanceof SQLException sqlException) {
                    throw new DatabaseException(operation + " failed; the transaction was rolled back.", sqlException);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DatabaseException(operation + " failed.", exception);
        }
    }

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }
}
