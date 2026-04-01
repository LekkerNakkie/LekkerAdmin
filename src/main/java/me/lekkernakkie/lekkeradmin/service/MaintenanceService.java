package me.lekkernakkie.lekkeradmin.service;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.MainConfig;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MaintenanceService {

    private final LekkerAdmin plugin;

    public MaintenanceService(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().getMainConfig().isMaintenanceEnabled();
    }

    public boolean toggle() {
        boolean newState = !isEnabled();
        setEnabled(newState);
        return newState;
    }

    public void setEnabled(boolean enabled) {
        plugin.getConfig().set("maintenance.enabled", enabled);
        plugin.saveConfig();
    }

    public boolean canJoin(Player player) {
        return !isEnabled() || hasBypass(player);
    }

    public boolean hasBypass(Player player) {
        return player.hasPermission("lekkeradmin.maintenance.bypass")
                || player.hasPermission("lekkeradmin.admin");
    }

    public void kickNonBypassOnlinePlayers() {
        MainConfig config = plugin.getConfigManager().getMainConfig();
        String kickMessage = color(config.getMaintenanceOnlineKickMessage());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasBypass(player)) {
                continue;
            }
            player.kickPlayer(kickMessage);
        }
    }

    public String getToggleOnMessage() {
        return color(plugin.getConfigManager().getMainConfig().getMaintenanceToggledOnMessage());
    }

    public String getToggleOffMessage() {
        return color(plugin.getConfigManager().getMainConfig().getMaintenanceToggledOffMessage());
    }

    public String getNoPermissionMessage() {
        return color(plugin.getConfigManager().getMainConfig().getMaintenanceNoPermissionMessage());
    }

    public String getJoinDenyMessage() {
        return color(plugin.getConfigManager().getMainConfig().getMaintenanceJoinKickMessage());
    }

    private String color(String text) {
        return PunishmentFormatter.colorize(text);
    }
}