package me.lekkernakkie.lekkeradmin.discord.util;

import net.dv8tion.jda.api.interactions.components.buttons.Button;

public final class DiscordButtonFactory {

    private DiscordButtonFactory() {
    }

    public static Button create(String style, String customId, String label) {
        String normalized = style == null ? "PRIMARY" : style.toUpperCase();

        return switch (normalized) {
            case "SUCCESS" -> Button.success(customId, label);
            case "DANGER" -> Button.danger(customId, label);
            case "SECONDARY" -> Button.secondary(customId, label);
            default -> Button.primary(customId, label);
        };
    }
}