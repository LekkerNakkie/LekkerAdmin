package me.lekkernakkie.lekkeradmin.discord.util;

import java.util.Map;

public final class DiscordPlaceholderUtil {

    private DiscordPlaceholderUtil() {
    }

    public static String apply(String input, Map<String, String> placeholders) {
        if (input == null) {
            return "";
        }

        String result = input;

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : entry.getValue();
                result = result.replace("{" + key + "}", value);
            }
        }

        return result;
    }
}