package me.lekkernakkie.lekkeradmin.util;

import org.bukkit.plugin.java.JavaPlugin;

public final class LoggerUtil {

    private LoggerUtil() {
    }

    public static void info(JavaPlugin plugin, String message) {
        if (plugin != null && message != null) {
            plugin.getLogger().info(message);
        }
    }

    public static void warning(JavaPlugin plugin, String message) {
        if (plugin != null && message != null) {
            plugin.getLogger().warning(message);
        }
    }

    public static void severe(JavaPlugin plugin, String message) {
        if (plugin != null && message != null) {
            plugin.getLogger().severe(message);
        }
    }
}