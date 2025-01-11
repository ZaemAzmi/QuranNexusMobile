package com.example.qurannexus.features.recitation.extensions;

public class TextUtils {
    public static String cleanArabicText(String input) {
        if (input == null) return "";

        // Remove or replace problematic Unicode characters
        return input.replaceAll("[\\p{Cf}\\p{Cn}\\p{Co}\\p{Cs}]", "") // Remove format and special characters
                .replaceAll("\\\\u[0-9a-fA-F]{4}", "") // Remove escaped Unicode
                .trim();
    }
}