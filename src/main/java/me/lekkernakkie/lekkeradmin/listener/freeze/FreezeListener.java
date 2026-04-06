package me.lekkernakkie.lekkeradmin.listener.freeze;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.freeze.FreezeService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public class FreezeListener implements Listener {

    private final LekkerAdmin plugin;
    private final FreezeService freezeService;

    public FreezeListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.freezeService = plugin.getFreezeService();
    }

    private boolean shouldIgnore(Player player) {
        return player == null
                || !freezeService.isFrozen(player)
                || player.hasPermission("lekkeradmin.freeze.bypass");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (shouldIgnore(player) || !freezeService.isMovementBlocked()) {
            return;
        }

        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }

        if (freezeService.isRotationOnlyAllowed()) {
            event.setTo(event.getFrom());
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (shouldIgnore(player) || !freezeService.isCommandsBlocked()) {
            return;
        }

        if (freezeService.isCommandAllowed(event.getMessage())) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(plugin.lang().message(
                "freeze.blocked-command",
                "{prefix} &cJe kan geen commands gebruiken terwijl je gefreezed bent."
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (shouldIgnore(event.getPlayer()) || !freezeService.isDropBlocked()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (shouldIgnore(player) || !freezeService.isPickupBlocked()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (shouldIgnore(player) || !freezeService.isInventoryBlocked()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (shouldIgnore(event.getPlayer()) || !freezeService.isInteractBlocked()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (shouldIgnore(event.getPlayer()) || !freezeService.isBlockBreakBlocked()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (shouldIgnore(event.getPlayer()) || !freezeService.isBlockPlaceBlocked()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (!shouldIgnore(player) && freezeService.isDamageDealBlocked()) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getEntity() instanceof Player player) {
            if (!shouldIgnore(player) && freezeService.isDamageTakeBlocked()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (shouldIgnore(event.getPlayer()) || !freezeService.isTeleportBlocked()) {
            return;
        }
        event.setCancelled(true);
    }
}