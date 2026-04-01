package me.lekkernakkie.lekkeradmin.model.punishment;

public enum PunishmentType {

    BAN,
    MUTE,
    KICK,
    WARN;

    public static PunishmentType fromString(String value) {
        for (PunishmentType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}