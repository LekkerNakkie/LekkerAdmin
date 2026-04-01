package me.lekkernakkie.lekkeradmin.model.log;

public enum ItemDestroyCause {
    LAVA,
    FIRE,
    FIRE_TICK,
    VOID,
    EXPLOSION,
    CACTUS,
    DESPAWN,
    UNKNOWN;

    public String getDisplayName() {
        return switch (this) {
            case LAVA -> "Lava";
            case FIRE -> "Fire";
            case FIRE_TICK -> "Fire Tick";
            case VOID -> "Void";
            case EXPLOSION -> "Explosion";
            case CACTUS -> "Cactus";
            case DESPAWN -> "Despawn";
            case UNKNOWN -> "Unknown";
        };
    }
}