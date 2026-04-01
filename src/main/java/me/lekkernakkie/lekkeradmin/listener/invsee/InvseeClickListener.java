package me.lekkernakkie.lekkeradmin.listener.invsee;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.invsee.InvseeSession;
import me.lekkernakkie.lekkeradmin.service.invsee.InvseeService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InvseeClickListener implements Listener {

    private final LekkerAdmin plugin;
    private final InvseeService invseeService;

    public InvseeClickListener(LekkerAdmin plugin, InvseeService invseeService) {
        this.plugin = plugin;
        this.invseeService = invseeService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!invseeService.isInvseeInventory(player, event.getView().getTopInventory())) {
            return;
        }

        InvseeSession session = invseeService.getSession(player);
        if (session == null) {
            return;
        }

        int rawSlot = event.getRawSlot();
        boolean topInventoryClick = rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize();

        if (topInventoryClick) {
            if (invseeService.isProtectedTopSlot(player, rawSlot)) {
                event.setCancelled(true);
                return;
            }

            if (session.isReadOnly()) {
                event.setCancelled(true);
                return;
            }
        }

        if (!event.isCancelled()) {
            recordClickAction(session, event, topInventoryClick);
        }

        if (!event.isCancelled() && session.isOnlineTarget()) {
            invseeService.scheduleOnlineSync(player);
        }
    }

    private void recordClickAction(InvseeSession session, InventoryClickEvent event, boolean topInventoryClick) {
        if (session.isReadOnly()) {
            return;
        }

        InventoryAction action = event.getAction();
        if (action == InventoryAction.NOTHING) {
            return;
        }

        String targetType = session.isEnderChest() ? "enderchest" : "inventory";
        String slotInfo = topInventoryClick ? "top slot " + event.getRawSlot() : "player inventory";
        String currentItem = describeItem(event.getCurrentItem());
        String cursorItem = describeItem(event.getCursor());

        String message = "Actie: " + action.name()
                + " | " + targetType
                + " | locatie: " + slotInfo
                + " | slot-item: " + currentItem
                + " | cursor: " + cursorItem;

        session.recordManualAction(message);
    }

    private String describeItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "AIR";
        }

        return item.getType().name() + " x" + item.getAmount();
    }
}