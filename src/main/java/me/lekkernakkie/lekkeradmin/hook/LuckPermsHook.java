package me.lekkernakkie.lekkeradmin.hook;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class LuckPermsHook {

    private final LekkerAdmin plugin;

    public LuckPermsHook(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void hook() {
        Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (luckPerms != null && luckPerms.isEnabled()) {
            plugin.getLogger().info("Hooked into LuckPerms.");
        } else {
            plugin.getLogger().info("LuckPerms not found, continuing without hook.");
        }
    }
}