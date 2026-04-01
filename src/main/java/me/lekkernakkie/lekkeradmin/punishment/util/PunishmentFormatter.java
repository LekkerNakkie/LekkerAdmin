package me.lekkernakkie.lekkeradmin.punishment.util;

import org.bukkit.ChatColor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class PunishmentFormatter {

    private PunishmentFormatter() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String apply(String template,
                               String player,
                               String actor,
                               String reason,
                               String duration,
                               String expiresAt,
                               String server) {
        return apply(template, player, actor, reason, duration, expiresAt, server, null);
    }

    public static String apply(String template,
                               String player,
                               String actor,
                               String reason,
                               String duration,
                               String expiresAt,
                               String server,
                               String date) {
        if (template == null) {
            return "";
        }

        String result = template;
        result = result.replace("{player}", safe(player));
        result = result.replace("{actor}", safe(actor));
        result = result.replace("{reason}", safe(reason));
        result = result.replace("{duration}", safe(duration));
        result = result.replace("{expires_at}", safe(expiresAt));
        result = result.replace("{server}", safe(server));
        result = result.replace("{date}", safe(date));
        result = result.replace("{remaining}", safe(duration));

        return colorize(result);
    }

    public static List<String> apply(List<String> templates,
                                     String player,
                                     String actor,
                                     String reason,
                                     String duration,
                                     String expiresAt,
                                     String server) {
        return apply(templates, player, actor, reason, duration, expiresAt, server, null);
    }

    public static List<String> apply(List<String> templates,
                                     String player,
                                     String actor,
                                     String reason,
                                     String duration,
                                     String expiresAt,
                                     String server,
                                     String date) {
        List<String> result = new ArrayList<>();
        if (templates == null) {
            return result;
        }

        for (String line : templates) {
            result.add(apply(line, player, actor, reason, duration, expiresAt, server, date));
        }
        return result;
    }

    public static String formatDuration(Long millis) {
        if (millis == null || millis <= 0L) {
            return "Permanent";
        }

        long totalSeconds = millis / 1000L;

        long weeks = totalSeconds / 604800L;
        totalSeconds %= 604800L;

        long days = totalSeconds / 86400L;
        totalSeconds %= 86400L;

        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;

        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder builder = new StringBuilder();
        if (weeks > 0) builder.append(weeks).append("w ");
        if (days > 0) builder.append(days).append("d ");
        if (hours > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");
        if (seconds > 0 && builder.isEmpty()) builder.append(seconds).append("s");

        String result = builder.toString().trim();
        return result.isBlank() ? "Permanent" : result;
    }

    public static String formatDate(Long timestamp, String pattern, String timezone) {
        if (timestamp == null || timestamp <= 0L) {
            return "Nooit";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.of(timezone));

        return formatter.format(Instant.ofEpochMilli(timestamp));
    }

    public static String formatRemaining(Long expiresAt, String pattern, String timezone) {
        if (expiresAt == null || expiresAt <= 0L) {
            return "Permanent";
        }

        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            return "Verlopen";
        }

        return formatDuration(remaining);
    }

    public static String valueOrUnknown(String input) {
        if (input == null || input.isBlank()) {
            return "Onbekend";
        }
        return input;
    }

    private static String safe(String input) {
        return input == null ? "" : input;
    }
}