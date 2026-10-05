package com.bloodmanagement.service;

import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.dao.UserDAO;
import com.bloodmanagement.exception.DuplicateRecordException;
import com.bloodmanagement.util.PasswordUtil;
import com.bloodmanagement.util.ValidationUtil;
import java.sql.SQLException;

public final class AccountService {
    private final UserDAO users;
    private final AuditLogDAO audit;

    public AccountService(UserDAO users, AuditLogDAO audit) {
        this.users = users;
        this.audit = audit;
    }

    public long createStaff(String username, char[] password, long actorId) {
        String safeUsername = ValidationUtil.username(username);
        if (password == null || password.length < 12) {
            throw new IllegalArgumentException("Password must contain at least 12 characters.");
        }
        try {
            long id = users.createStaffAccount(safeUsername, PasswordUtil.hash(password));
            audit.record(actorId, "STAFF_ACCOUNT_CREATED", "Created staff account #" + id);
            return id;
        } catch (com.bloodmanagement.exception.DatabaseException exception) {
            if (exception.getCause() instanceof SQLException sql && "23000".equals(sql.getSQLState())) {
                throw new DuplicateRecordException("That username is already in use.");
            }
            throw exception;
        }
    }
}
