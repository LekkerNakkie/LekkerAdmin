package me.lekkernakkie.lekkeradmin.listener.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.log.ItemDestroyCause;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;

public class TrackedItemDestroyListener implements Listener {

    private final LekkerAdmin plugin;

    public TrackedItemDestroyListener(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (!isLogsSystemEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Item item)) {
            return;
        }

        ItemDestroyCause cause = mapCause(event.getCause());
        if (cause == null) {
            return;
        }

        plugin.getItemLogAggregationService().markDestroyed(item.getUniqueId(), cause);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (!isLogsSystemEnabled()) {
            return;
        }

        if (event.getEntity() == null || event.getEntity().getItemStack().getType() == Material.AIR) {
            return;
        }

        plugin.getItemLogAggregationService().markDestroyed(
                event.getEntity().getUniqueId(),
                ItemDestroyCause.DESPAWN
        );
    }

    private boolean isLogsSystemEnabled() {
        return plugin.getConfigManager() != null
                && plugin.getConfigManager().getLogsConfig() != null
                && plugin.getConfigManager().getLogsConfig().isEnabled()
                && plugin.getItemLogAggregationService() != null;
    }

    private ItemDestroyCause mapCause(EntityDamageEvent.DamageCause cause) {
        if (cause == null) {
            return ItemDestroyCause.UNKNOWN;
        }

        return switch (cause) {
            case LAVA -> ItemDestroyCause.LAVA;
            case FIRE -> ItemDestroyCause.FIRE;
            case FIRE_TICK -> ItemDestroyCause.FIRE_TICK;
            case VOID -> ItemDestroyCause.VOID;
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> ItemDestroyCause.EXPLOSION;
            case CONTACT -> ItemDestroyCause.CACTUS;
            default -> null;
        };
    }
}