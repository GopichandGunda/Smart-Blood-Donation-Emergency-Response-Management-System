package com.bloodmanagement.util;

import java.util.regex.Pattern;

public final class ValidationUtil {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 ()-]{6,19}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,80}$");

    private ValidationUtil() {
    }

    public static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    public static void email(String value) {
        if (value == null || !EMAIL.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
    }

    public static void phone(String value) {
        if (value == null || !PHONE.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Enter a valid phone number.");
        }
    }

    public static void positive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero.");
        }
    }

    public static String username(String value) {
        String clean = required(value, "Username");
        if (!USERNAME.matcher(clean).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-80 characters and contain only letters, numbers, dots, underscores, or hyphens.");
        }
        return clean;
    }

    public static void age(int value) {
        if (value < 18 || value > 100) {
            throw new IllegalArgumentException("Donors must be between 18 and 100 years old.");
        }
    }
}
