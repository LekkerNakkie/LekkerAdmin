package me.lekkernakkie.lekkeradmin.discord.log;

public enum MinecraftLogDeliveryMode {
    BOT,
    WEBHOOK,
    BOTH,
    NONE;

    public static MinecraftLogDeliveryMode fromString(String input) {
        if (input == null || input.isBlank()) {
            return BOT;
        }

        try {
            return MinecraftLogDeliveryMode.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return BOT;
        }
    }
}