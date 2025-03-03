package com.example.qurannexus.features.recitation.extensions;

public class TextUtils {
    public static String cleanArabicText(String input) {
        if (input == null) return "";

        try {
            // IMPORTANT: DO NOT filter out any Arabic characters or diacritics
            // Only remove invisible control characters that might break display
            return input.replaceAll("[\\p{Cc}]", "")  // Remove only control characters
                    .trim();
        } catch (Exception e) {
            return input;  // Return original input if cleaning fails
        }
    }
}