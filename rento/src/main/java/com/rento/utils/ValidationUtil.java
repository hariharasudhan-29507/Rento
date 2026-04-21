package com.rento.utils;

import java.util.regex.Pattern;

/**
 * Input validation utilities for forms and data.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[+]?[0-9]{10,15}$");

    private static final Pattern CARD_NUMBER_PATTERN =
        Pattern.compile("^[0-9]{13,19}$");

    private static final Pattern CVV_PATTERN =
        Pattern.compile("^[0-9]{3,4}$");

    private static final Pattern NAME_PATTERN =
        Pattern.compile("^[A-Za-z\\s]{2,50}$");

    /**
     * Validate password strength.
     * @return error message or null if valid
     */
    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain an uppercase letter";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain a lowercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain a number";
        }
        if (!password.matches(".*[@$!%*?&#^()\\-_+=].*")) {
            return "Password must contain a special character (@$!%*?&#)";
        }
        return null; // valid
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String cleaned = phone.replaceAll("[\\s\\-()]", "");
        return PHONE_PATTERN.matcher(cleaned).matches();
    }

    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }

    public static boolean isValidAge(int age) {
        return age >= 18 && age <= 90;
    }

    public static boolean isValidCardNumber(String number) {
        if (number == null) return false;
        String cleaned = number.replaceAll("[\\s\\-]", "");
        if (!CARD_NUMBER_PATTERN.matcher(cleaned).matches()) return false;
        return luhnCheck(cleaned);
    }

    public static boolean isValidCVV(String cvv) {
        return cvv != null && CVV_PATTERN.matcher(cvv).matches();
    }

    public static boolean isValidExpiryDate(String expiry) {
        if (expiry == null) return false;
        String[] parts = expiry.split("/");
        if (parts.length != 2) return false;
        try {
            int month = Integer.parseInt(parts[0].trim());
            int year = Integer.parseInt(parts[1].trim());
            return month >= 1 && month <= 12 && year >= 24 && year <= 40;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Luhn algorithm for card number validation.
     */
    private static boolean luhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /**
     * Mask a card number showing only last 4 digits.
     */
    public static String maskCardNumber(String number) {
        if (number == null || number.length() < 4) return "****";
        String cleaned = number.replaceAll("[\\s\\-]", "");
        return "**** **** **** " + cleaned.substring(cleaned.length() - 4);
    }

    /**
     * Format currency amount.
     */
    public static String formatCurrency(double amount) {
        return String.format("₹%.2f", amount);
    }
}
