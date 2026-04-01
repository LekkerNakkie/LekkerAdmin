package me.lekkernakkie.lekkeradmin.model.punishment;

public enum PunishmentStatus {

    ACTIVE,
    EXPIRED,
    REMOVED;

    public static PunishmentStatus fromString(String value) {
        for (PunishmentStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

}