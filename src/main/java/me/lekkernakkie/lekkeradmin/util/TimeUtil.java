package me.lekkernakkie.lekkeradmin.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Brussels"));

    private TimeUtil() {
    }

    public static String formatMillis(long millis) {
        return FORMATTER.format(Instant.ofEpochMilli(millis));
    }
}