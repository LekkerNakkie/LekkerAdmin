package me.lekkernakkie.lekkeradmin.discord.embed;

import java.awt.Color;

public abstract class EmbedFactory {

    protected Color parseColor(String hex, Color fallback) {
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