package me.lekkernakkie.lekkeradmin.service;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

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
        kickNonBypassOnlinePlayers((UUID) null);
    }

    public void kickNonBypassOnlinePlayers(Player exemptPlayer) {
        kickNonBypassOnlinePlayers(exemptPlayer == null ? null : exemptPlayer.getUniqueId());
    }

    public void kickNonBypassOnlinePlayers(UUID exemptPlayerUuid) {
        String kickMessage = plugin.lang().get(
                "maintenance.kick-online",
                "&cServer staat in maintenance mode.\n\n&7Probeer later opnieuw."
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (exemptPlayerUuid != null && player.getUniqueId().equals(exemptPlayerUuid)) {
                continue;
            }

            if (hasBypass(player)) {
                continue;
            }

            player.kickPlayer(kickMessage);
        }
    }

    public String getToggleOnMessage() {
        return plugin.lang().message(
                "maintenance.enabled",
                "&7Maintenance mode is nu &aingeschakeld&7."
        );
    }

    public String getToggleOffMessage() {
        return plugin.lang().message(
                "maintenance.disabled",
                "&7Maintenance mode is nu &cuitgeschakeld&7."
        );
    }

    public String getNoPermissionMessage() {
        return plugin.lang().message(
                "general.no-permission",
                "&cDaar edde gij het lef ni vur.."
        );
    }

    public String getJoinDenyMessage() {
        return plugin.lang().get(
                "maintenance.kick-join",
                "&cServer staat in maintenance mode.\n\n&7Probeer later opnieuw."
        );
    }
}