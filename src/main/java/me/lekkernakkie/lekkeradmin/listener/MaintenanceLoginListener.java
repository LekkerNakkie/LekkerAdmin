package me.lekkernakkie.lekkeradmin.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.MaintenanceService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class MaintenanceLoginListener implements Listener {

    private final MaintenanceService maintenanceService;

    public MaintenanceLoginListener(LekkerAdmin plugin) {
        this.maintenanceService = plugin.getMaintenanceService();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!maintenanceService.isEnabled()) {
            return;
        }

        if (maintenanceService.hasBypass(event.getPlayer())) {
            return;
        }

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, maintenanceService.getJoinDenyMessage());
    }
}