package me.lekkernakkie.lekkeradmin.model.punishment;

public enum PunishmentSource {

    MINECRAFT,
    DISCORD,
    CONSOLE,
    SYSTEM;

    public static PunishmentSource fromString(String value) {
        for (PunishmentSource source : values()) {
            if (source.name().equalsIgnoreCase(value)) {
                return source;
            }
        }
        return null;
    }

}