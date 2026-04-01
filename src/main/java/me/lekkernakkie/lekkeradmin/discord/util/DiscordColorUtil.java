package me.lekkernakkie.lekkeradmin.discord.util;

import java.awt.Color;

public final class DiscordColorUtil {

    private DiscordColorUtil() {
    }

    public static Color fromHex(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) {
            return fallback;
        }

        try {
            return Color.decode(hex);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}