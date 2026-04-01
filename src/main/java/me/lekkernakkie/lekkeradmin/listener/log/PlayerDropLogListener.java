package me.lekkernakkie.lekkeradmin.listener.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogFilterConfig;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.model.log.LoggedItemData;
import me.lekkernakkie.lekkeradmin.util.log.ItemLogUtil;
import me.lekkernakkie.lekkeradmin.util.log.LocationLogUtil;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerDropLogListener implements Listener {

    private final LekkerAdmin plugin;

    public PlayerDropLogListener(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return;
        }

        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getPlayerDrops();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Item droppedEntity = event.getItemDrop();
        ItemStack itemStack = droppedEntity == null ? null : droppedEntity.getItemStack();

        if (player == null || droppedEntity == null || itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        LogFilterConfig filter = settings.getFilterConfig();
        if (filter == null) {
            return;
        }

        if (isIgnoredWorld(player.getWorld().getName(), filter)) {
            return;
        }

        if (isIgnoredMaterial(itemStack.getType(), filter)) {
            return;
        }

        LoggedItemData itemData = ItemLogUtil.fromItemStack(itemStack);
        if (itemData.getAmount() < filter.getMinAmount()) {
            return;
        }

        if (filter.isOnlyLogIfEnchanted() && !itemData.isEnchanted()) {
            return;
        }

        plugin.getItemLogAggregationService().addDrop(
                player,
                droppedEntity.getUniqueId(),
                itemData,
                settings,
                LocationLogUtil.formatWorld(player.getLocation()),
                LocationLogUtil.formatCoordinates(player.getLocation())
        );
    }

    private boolean isIgnoredWorld(String worldName, LogFilterConfig filter) {
        if (worldName == null || filter.getIgnoredWorlds() == null) {
            return false;
        }

        return filter.getIgnoredWorlds().stream()
                .anyMatch(world -> world != null && world.equalsIgnoreCase(worldName));
    }

    private boolean isIgnoredMaterial(Material material, LogFilterConfig filter) {
        if (material == null || filter.getIgnoredMaterials() == null) {
            return false;
        }

        return filter.getIgnoredMaterials().stream()
                .anyMatch(name -> name != null && name.equalsIgnoreCase(material.name()));
    }
}