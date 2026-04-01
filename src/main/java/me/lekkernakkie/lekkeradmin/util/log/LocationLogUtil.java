package me.lekkernakkie.lekkeradmin.util.log;

import org.bukkit.Location;

public final class LocationLogUtil {

    private LocationLogUtil() {
    }

    public static String formatWorld(Location location) {
        if (location == null || location.getWorld() == null) {
            return "-";
        }

        return location.getWorld().getName();
    }

    public static String formatCoordinates(Location location) {
        if (location == null) {
            return "-";
        }

        return "X: " + location.getBlockX()
                + ", Y: " + location.getBlockY()
                + ", Z: " + location.getBlockZ();
    }
}