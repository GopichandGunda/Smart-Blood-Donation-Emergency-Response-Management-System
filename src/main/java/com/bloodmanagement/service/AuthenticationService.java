package com.bloodmanagement.service;

import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.dao.UserDAO;
import com.bloodmanagement.exception.AuthenticationException;
import com.bloodmanagement.model.User;
import com.bloodmanagement.util.PasswordUtil;
import com.bloodmanagement.util.ValidationUtil;

public final class AuthenticationService {
    private final UserDAO users;
    private final AuditLogDAO audit;

    public AuthenticationService(UserDAO users, AuditLogDAO audit) {
        this.users = users;
        this.audit = audit;
    }

    public User authenticate(String username, char[] password) {
        String normalized = ValidationUtil.required(username, "Username");
        var account = users.findByUsername(normalized);
        if (account.isEmpty() || !account.get().active()
                || !PasswordUtil.verify(password, account.get().passwordHash())) {
            throw new AuthenticationException("Invalid username or password, or the account is inactive.");
        }
        User user = account.get();
        audit.record(user.id(), "LOGIN", "Successful login");
        return user;
    }
}
