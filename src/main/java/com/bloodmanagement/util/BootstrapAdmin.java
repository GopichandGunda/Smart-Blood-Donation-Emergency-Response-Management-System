package com.bloodmanagement.util;

import com.bloodmanagement.dao.UserDAO;
import java.io.Console;

public final class BootstrapAdmin {
    private BootstrapAdmin() {
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Run this command in a terminal that supports secure password entry.");
            System.exit(1);
        }
        String username = console.readLine("New administrator username: ");
        char[] password = console.readPassword("New administrator password (12+ characters): ");
        char[] confirmation = console.readPassword("Confirm password: ");
        try {
            if (!java.util.Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            String hash = PasswordUtil.hash(password);
            new UserDAO().createAdmin(ValidationUtil.username(username), hash);
            console.printf("Administrator '%s' created successfully.%n", username.trim());
        } catch (RuntimeException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        } finally {
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(confirmation, '\0');
        }
    }
}
