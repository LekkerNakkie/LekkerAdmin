package me.lekkernakkie.lekkeradmin.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

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

    public static void exception(JavaPlugin plugin, Level level, String message, Throwable throwable) {
        if (plugin == null || message == null) {
            return;
        }

        if (throwable == null) {
            plugin.getLogger().log(level, message);
            return;
        }

        plugin.getLogger().log(level, message, throwable);
    }
}
