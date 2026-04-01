package me.lekkernakkie.lekkeradmin.listener.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogFilterConfig;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.model.log.LoggedItemData;
import me.lekkernakkie.lekkeradmin.util.log.DeathLogUtil;
import me.lekkernakkie.lekkeradmin.util.log.ItemLogUtil;
import me.lekkernakkie.lekkeradmin.util.log.LocationLogUtil;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDeathLogListener implements Listener {

    private final LekkerAdmin plugin;

    public PlayerDeathLogListener(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return;
        }

        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getPlayerDeaths();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        Player player = event.getEntity();
        if (player == null) {
            return;
        }

        LogFilterConfig filter = settings.getFilterConfig();
        if (filter == null) {
            return;
        }

        if (isIgnoredWorld(player.getWorld().getName(), filter)) {
            return;
        }

        String cause = DeathLogUtil.formatCause(
                player.getLastDamageCause() == null ? null : player.getLastDamageCause().getCause()
        );
        String killer = DeathLogUtil.resolveKiller(player);

        String xpSummary = "Dropped XP: " + event.getDroppedExp() + " | Level: " + player.getLevel();

        boolean keepInventory = Boolean.TRUE.equals(
                player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY)
        );

        List<LoggedItemData> loggedItems = new ArrayList<>();
        List<UUID> fakeEntityIds = new ArrayList<>();

        for (ItemStack drop : event.getDrops()) {
            if (drop == null || drop.getType() == Material.AIR) {
                continue;
            }

            if (isIgnoredMaterial(drop.getType(), filter)) {
                continue;
            }

            LoggedItemData itemData = ItemLogUtil.fromItemStack(drop);

            if (itemData.getAmount() < filter.getMinAmount()) {
                continue;
            }

            if (filter.isOnlyLogIfEnchanted() && !itemData.isEnchanted()) {
                continue;
            }

            loggedItems.add(itemData);
            fakeEntityIds.add(UUID.randomUUID());
        }

        plugin.getItemLogAggregationService().addDeath(
                player,
                fakeEntityIds,
                loggedItems,
                settings,
                LocationLogUtil.formatWorld(player.getLocation()),
                LocationLogUtil.formatCoordinates(player.getLocation()),
                cause,
                killer,
                xpSummary,
                keepInventory
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