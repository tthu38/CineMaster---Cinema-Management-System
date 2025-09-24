package com.example.cinemaster.util;

public class LoginUtil {

    private LoginUtil() {
        // Ngăn tạo instance
    }
    public static String normalizePhoneVN(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");

        if (digits.matches("^0\\d{9}$")) {
            return digits;
        }

        return null;
    }
}
