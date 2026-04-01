package me.lekkernakkie.lekkeradmin.punishment.util;

import me.lekkernakkie.lekkeradmin.config.PunishmentsConfig;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PunishmentDurationParser {

    private static final Pattern PART_PATTERN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    private PunishmentDurationParser() {
    }

    public static ParseResult parse(String input, PunishmentsConfig config) {
        if (input == null || input.isBlank()) {
            return new ParseResult(false, null, false);
        }

        String normalized = input.trim().toLowerCase(Locale.ROOT);

        List<String> permanentKeywords = config.getPermanentKeywords();
        for (String keyword : permanentKeywords) {
            if (normalized.equalsIgnoreCase(keyword)) {
                return new ParseResult(true, null, true);
            }
        }

        Matcher matcher = PART_PATTERN.matcher(normalized);
        long totalMs = 0L;
        int matches = 0;

        while (matcher.find()) {
            matches++;

            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);

            switch (unit) {
                case "s" -> totalMs += amount * 1000L;
                case "m" -> totalMs += amount * 60_000L;
                case "h" -> totalMs += amount * 3_600_000L;
                case "d" -> totalMs += amount * 86_400_000L;
                case "w" -> totalMs += amount * 604_800_000L;
                default -> {
                    return new ParseResult(false, null, false);
                }
            }
        }

        String consumed = normalized.replaceAll("(\\d+)([smhdw])", "");
        if (matches == 0 || !consumed.isBlank() || totalMs <= 0L) {
            return new ParseResult(false, null, false);
        }

        return new ParseResult(true, totalMs, false);
    }

    public record ParseResult(boolean valid, Long durationMs, boolean permanent) {
    }
}