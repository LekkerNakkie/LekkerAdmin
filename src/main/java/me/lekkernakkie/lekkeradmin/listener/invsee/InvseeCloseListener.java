package me.lekkernakkie.lekkeradmin.listener.invsee;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.invsee.InvseeService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InvseeCloseListener implements Listener {

    private final LekkerAdmin plugin;
    private final InvseeService invseeService;

    public InvseeCloseListener(LekkerAdmin plugin, InvseeService invseeService) {
        this.plugin = plugin;
        this.invseeService = invseeService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (!invseeService.isInvseeInventory(player, event.getInventory())) {
            return;
        }

        invseeService.closeSession(player);
    }
}