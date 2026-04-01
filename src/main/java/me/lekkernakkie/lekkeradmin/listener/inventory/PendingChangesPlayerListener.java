package me.lekkernakkie.lekkeradmin.listener.inventory;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.invsee.OfflineInventoryService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PendingChangesPlayerListener implements Listener {

    private final LekkerAdmin plugin;
    private final OfflineInventoryService offlineInventoryService;

    public PendingChangesPlayerListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.offlineInventoryService = new OfflineInventoryService(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                // Eerst open offline edit sessies van deze target flushen/sluiten
                plugin.getInvseeService().handleTargetJoinWhileOfflineEditing(player);

                // Daarna pending wijzigingen op de echte speler toepassen
                offlineInventoryService.applyPendingChanges(player);

                // En tenslotte de nieuwe online snapshot opslaan
                offlineInventoryService.saveOnlineSnapshot(player);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to process pending_changes on join for " + player.getName() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        try {
            offlineInventoryService.saveOnlineSnapshot(event.getPlayer());
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save pending_changes snapshot on quit for " + event.getPlayer().getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        try {
            offlineInventoryService.saveOnlineSnapshot(event.getPlayer());
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save pending_changes snapshot on kick for " + event.getPlayer().getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}