package me.lekkernakkie.lekkeradmin.util;

public final class StringUtil {

    private StringUtil() {
    }

    public static String colorize(String input) {
        return input == null ? "" : input.replace("&", "§");
    }

    public static String safe(String input) {
        return input == null || input.isBlank() ? "-" : input;
    }

    public static String limit(String input, int maxLength) {
        if (input == null) {
            return "-";
        }

        if (maxLength <= 3 || input.length() <= maxLength) {
            return input;
        }

        return input.substring(0, maxLength - 3) + "...";
    }
}