package me.lekkernakkie.lekkeradmin.listener.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

public class TrackedItemSpawnLinkListener implements Listener {

    private final LekkerAdmin plugin;

    public TrackedItemSpawnLinkListener(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (plugin.getItemLogAggregationService() == null) {
            return;
        }

        Item item = event.getEntity();
        if (item == null || item.getItemStack() == null) {
            return;
        }

        plugin.getItemLogAggregationService().tryLinkSpawnedDeathItem(item);
    }
}