package me.lekkernakkie.lekkeradmin.hook;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PlaceholderApiHook {

    private final LekkerAdmin plugin;

    public PlaceholderApiHook(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void hook() {
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled()) {
            plugin.getLogger().info("Hooked into PlaceholderAPI.");
        } else {
            plugin.getLogger().info("PlaceholderAPI not found, continuing without hook.");
        }
    }
}