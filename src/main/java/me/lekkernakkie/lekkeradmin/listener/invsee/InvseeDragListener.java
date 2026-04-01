package me.lekkernakkie.lekkeradmin.listener.invsee;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.invsee.InvseeSession;
import me.lekkernakkie.lekkeradmin.service.invsee.InvseeService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InvseeDragListener implements Listener {

    private final LekkerAdmin plugin;
    private final InvseeService invseeService;

    public InvseeDragListener(LekkerAdmin plugin, InvseeService invseeService) {
        this.plugin = plugin;
        this.invseeService = invseeService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
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

        List<Integer> touchedTopSlots = new ArrayList<>();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize()) {
                if (invseeService.isProtectedTopSlot(player, rawSlot) || session.isReadOnly()) {
                    event.setCancelled(true);
                    return;
                }

                touchedTopSlots.add(rawSlot);
            }
        }

        if (!event.isCancelled() && !touchedTopSlots.isEmpty()) {
            recordDragAction(session, touchedTopSlots, event.getOldCursor());
        }

        if (!event.isCancelled() && session.isOnlineTarget()) {
            invseeService.scheduleOnlineSync(player);
        }
    }

    private void recordDragAction(InvseeSession session, List<Integer> touchedTopSlots, ItemStack oldCursor) {
        if (session.isReadOnly()) {
            return;
        }

        String targetType = session.isEnderChest() ? "enderchest" : "inventory";
        String cursorItem = describeItem(oldCursor);

        session.recordManualAction(
                "Drag actie | " + targetType
                        + " | slots: " + touchedTopSlots
                        + " | cursor: " + cursorItem
        );
    }

    private String describeItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "AIR";
        }

        return item.getType().name() + " x" + item.getAmount();
    }
}