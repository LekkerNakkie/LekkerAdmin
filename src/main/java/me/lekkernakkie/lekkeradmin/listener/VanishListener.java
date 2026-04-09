package me.lekkernakkie.lekkeradmin.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.vanish.VanishService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VanishListener implements Listener {

    private final LekkerAdmin plugin;
    private final VanishService vanishService;

    public VanishListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.vanishService = plugin.getVanishService();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();

        if (plugin.getConfigManager().getMainConfig().isVanishSuppressJoinMessage()
                && vanishService.isVanished(joined.getUniqueId())) {
            event.joinMessage(null);
        }

        vanishService.handleJoin(joined);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfigManager().getMainConfig().isVanishSuppressQuitMessage()
                && vanishService.isVanished(player.getUniqueId())) {
            event.quitMessage(null);
        }

        vanishService.handleQuit(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMobTarget(EntityTargetEvent event) {
        if (!plugin.getConfigManager().getMainConfig().isVanishBlockMobTargeting()) {
            return;
        }

        if (!(event.getTarget() instanceof Player player)) {
            return;
        }

        if (vanishService.isVanished(player.getUniqueId())) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfigManager().getMainConfig().isVanishBlockItemPickup()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (vanishService.isVanished(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfigManager().getMainConfig().isVanishHideDroppedItems()) {
            return;
        }

        Player player = event.getPlayer();
        if (!vanishService.isVanished(player.getUniqueId())) {
            return;
        }

        Item droppedItem = event.getItemDrop();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!droppedItem.isValid()) {
                return;
            }

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }

                if (canSeeVanished(viewer)) {
                    viewer.showEntity(plugin, droppedItem);
                } else {
                    viewer.hideEntity(plugin, droppedItem);
                }
            }
        });
    }

    private boolean canSeeVanished(Player viewer) {
        return viewer.hasPermission("lekkeradmin.admin")
                || viewer.hasPermission("lekkeradmin.vanish.see");
    }
}