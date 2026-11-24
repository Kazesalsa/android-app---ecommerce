package com.example.btday4.util;

import java.util.regex.Pattern;

/**
 * Pure input validation helpers shared across Auth, Profile, Checkout, and Reviews.
 */
public final class ValidationUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+?84|0)[0-9]{8,10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtils() {
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static String normalizePhone(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        return rawPhone.replaceAll("[\\s.-]", "").trim();
    }

    public static boolean isValidPhone(String rawPhone) {
        String normalized = normalizePhone(rawPhone);
        return PHONE_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidEmail(String rawEmail) {
        if (rawEmail == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(rawEmail.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 4;
    }

    public static boolean isValidBirthDate(int day, int month, int year, int currentYear) {
        if (year < 1920 || year > currentYear || month < 1 || month > 12 || day < 1) {
            return false;
        }
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        boolean isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
        int maxDay = (month == 2 && isLeap) ? 29 : daysInMonth[month - 1];
        return day <= maxDay;
    }
}
