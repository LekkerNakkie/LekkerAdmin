package me.lekkernakkie.lekkeradmin.util.log;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public final class DeathLogUtil {

    private DeathLogUtil() {
    }

    public static String formatCause(EntityDamageEvent.DamageCause cause) {
        if (cause == null) {
            return "Onbekend";
        }

        return switch (cause) {
            case ENTITY_ATTACK -> "Entity Attack";
            case ENTITY_SWEEP_ATTACK -> "Sweep Attack";
            case PROJECTILE -> "Projectile";
            case SUFFOCATION -> "Suffocation";
            case FALL -> "Fall";
            case FIRE -> "Fire";
            case FIRE_TICK -> "Fire Tick";
            case MELTING -> "Melting";
            case LAVA -> "Lava";
            case DROWNING -> "Drowning";
            case BLOCK_EXPLOSION -> "Block Explosion";
            case ENTITY_EXPLOSION -> "Entity Explosion";
            case VOID -> "Void";
            case LIGHTNING -> "Lightning";
            case SUICIDE -> "Suicide";
            case STARVATION -> "Starvation";
            case POISON -> "Poison";
            case MAGIC -> "Magic";
            case WITHER -> "Wither";
            case FALLING_BLOCK -> "Falling Block";
            case THORNS -> "Thorns";
            case DRAGON_BREATH -> "Dragon Breath";
            case CUSTOM -> "Custom";
            case FLY_INTO_WALL -> "Fly Into Wall";
            case HOT_FLOOR -> "Hot Floor";
            case CRAMMING -> "Cramming";
            case DRYOUT -> "Dryout";
            case FREEZE -> "Freeze";
            case SONIC_BOOM -> "Sonic Boom";
            case WORLD_BORDER -> "World Border";
            case KILL -> "Kill";
            default -> toPretty(cause.name());
        };
    }

    public static String resolveKiller(Player player) {
        if (player == null) {
            return "-";
        }

        Player killer = player.getKiller();
        if (killer != null) {
            return killer.getName();
        }

        Entity lastDamager = player.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntity
                ? byEntity.getDamager()
                : null;

        if (lastDamager != null) {
            return lastDamager.getType().name();
        }

        return "-";
    }

    private static String toPretty(String input) {
        String lower = input.toLowerCase().replace("_", " ");
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(" ");
            }

            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }

        return builder.isEmpty() ? "-" : builder.toString();
    }
}