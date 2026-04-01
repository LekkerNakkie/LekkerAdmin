package me.lekkernakkie.lekkeradmin.discord.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogAggregationConfig;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.model.log.ItemDestroyCause;
import me.lekkernakkie.lekkeradmin.model.log.ItemLogBatch;
import me.lekkernakkie.lekkeradmin.model.log.ItemLogBatchEntry;
import me.lekkernakkie.lekkeradmin.model.log.ItemLogBatchType;
import me.lekkernakkie.lekkeradmin.model.log.LoggedItemData;
import me.lekkernakkie.lekkeradmin.model.log.MergedItem;
import me.lekkernakkie.lekkeradmin.model.log.PlayerDeathLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerDropLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerPickupLogContext;
import me.lekkernakkie.lekkeradmin.util.log.BatchLogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemLogAggregationService {

    private final LekkerAdmin plugin;
    private final MinecraftLogDispatcher dispatcher;

    private final Map<UUID, ItemLogBatch> dropBatchesByPlayer;
    private final Map<UUID, ItemLogBatch> pickupBatchesByPlayer;
    private final Map<UUID, ItemLogBatch> deathBatchesByPlayer;
    private final Map<UUID, ItemLogBatch> trackedEntities;

    public ItemLogAggregationService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.dispatcher = new MinecraftLogDispatcher(plugin);
        this.dropBatchesByPlayer = new ConcurrentHashMap<>();
        this.pickupBatchesByPlayer = new ConcurrentHashMap<>();
        this.deathBatchesByPlayer = new ConcurrentHashMap<>();
        this.trackedEntities = new ConcurrentHashMap<>();
    }

    public void addDrop(Player player, UUID entityUuid, LoggedItemData itemData, LogTypeSettings settings, String worldName, String coordinates) {
        if (player == null || entityUuid == null || itemData == null || settings == null) {
            return;
        }

        LogAggregationConfig aggregation = settings.getAggregationConfig();
        if (aggregation == null || !aggregation.isEnabled()) {
            dispatchSingleDrop(player, itemData, settings, worldName, coordinates);
            return;
        }

        ItemLogBatch batch = dropBatchesByPlayer.computeIfAbsent(
                player.getUniqueId(),
                uuid -> createBatch(player, worldName, coordinates, ItemLogBatchType.DROP)
        );

        batch.addEntry(new ItemLogBatchEntry(entityUuid, itemData));

        if (aggregation.isTrackDestruction() || aggregation.isTrackPickups()) {
            trackedEntities.put(entityUuid, batch);
        }

        scheduleFlush(batch, settings, dropBatchesByPlayer, player.getUniqueId(), aggregation.getFlushAfterTicks());
    }

    public void addPickup(Player player, UUID entityUuid, LoggedItemData itemData, LogTypeSettings settings, String worldName, String coordinates) {
        if (player == null || entityUuid == null || itemData == null || settings == null) {
            return;
        }

        LogAggregationConfig aggregation = settings.getAggregationConfig();
        if (aggregation == null || !aggregation.isEnabled()) {
            dispatchSinglePickup(player, itemData, settings, worldName, coordinates);
            return;
        }

        ItemLogBatch batch = pickupBatchesByPlayer.computeIfAbsent(
                player.getUniqueId(),
                uuid -> createBatch(player, worldName, coordinates, ItemLogBatchType.PICKUP)
        );

        batch.addEntry(new ItemLogBatchEntry(entityUuid, itemData));

        if (aggregation.isTrackDestruction() || aggregation.isTrackPickups()) {
            trackedEntities.put(entityUuid, batch);
        }

        scheduleFlush(batch, settings, pickupBatchesByPlayer, player.getUniqueId(), aggregation.getFlushAfterTicks());
    }

    public void addDeath(
            Player player,
            List<UUID> entityUuids,
            List<LoggedItemData> items,
            LogTypeSettings settings,
            String worldName,
            String coordinates,
            String cause,
            String killer,
            String xpSummary,
            boolean keepInventory
    ) {
        if (player == null || settings == null) {
            return;
        }

        LogAggregationConfig aggregation = settings.getAggregationConfig();
        if (aggregation == null || !aggregation.isEnabled()) {
            dispatchSingleDeath(player, items, settings, worldName, coordinates, cause, killer, xpSummary, keepInventory);
            return;
        }

        ItemLogBatch batch = deathBatchesByPlayer.computeIfAbsent(
                player.getUniqueId(),
                uuid -> createBatch(player, worldName, coordinates, ItemLogBatchType.DEATH)
        );

        batch.setDeathCause(cause);
        batch.setKillerName(killer);
        batch.setXpSummary(xpSummary);
        batch.setKeepInventory(keepInventory);

        if (entityUuids != null && items != null) {
            int size = Math.min(entityUuids.size(), items.size());
            for (int i = 0; i < size; i++) {
                UUID entityUuid = entityUuids.get(i);
                LoggedItemData itemData = items.get(i);

                if (itemData == null) {
                    continue;
                }

                ItemLogBatchEntry entry = new ItemLogBatchEntry(entityUuid, itemData);
                batch.addEntry(entry);

                if (entityUuid != null && (aggregation.isTrackDestruction() || aggregation.isTrackPickups())) {
                    trackedEntities.put(entityUuid, batch);
                }
            }
        }

        scheduleFlush(batch, settings, deathBatchesByPlayer, player.getUniqueId(), aggregation.getFlushAfterTicks());
    }

    public void tryLinkSpawnedDeathItem(Item item) {
        if (item == null || item.getItemStack() == null) {
            return;
        }

        Location itemLocation = item.getLocation();
        if (itemLocation == null || itemLocation.getWorld() == null) {
            return;
        }

        for (ItemLogBatch batch : deathBatchesByPlayer.values()) {
            if (batch == null) {
                continue;
            }

            LogTypeSettings settings = getSettingsForBatch(batch);
            if (settings == null || settings.getAggregationConfig() == null || !settings.getAggregationConfig().isTrackDestruction()) {
                continue;
            }

            if (!batch.getWorldName().equalsIgnoreCase(itemLocation.getWorld().getName())) {
                continue;
            }

            if (System.currentTimeMillis() - batch.getCreatedAt() > 5000L) {
                continue;
            }

            ItemLogBatchEntry match = findUnlinkedMatchingEntry(batch, item);
            if (match == null) {
                continue;
            }

            match.setEntityUuid(item.getUniqueId());
            trackedEntities.put(item.getUniqueId(), batch);
            return;
        }
    }

    private ItemLogBatchEntry findUnlinkedMatchingEntry(ItemLogBatch batch, Item item) {
        String material = item.getItemStack().getType().name();
        int amount = item.getItemStack().getAmount();

        for (ItemLogBatchEntry entry : batch.getEntries()) {
            if (entry == null || entry.getItemData() == null) {
                continue;
            }

            if (entry.getEntityUuid() != null) {
                continue;
            }

            LoggedItemData data = entry.getItemData();
            if (!data.getMaterial().equalsIgnoreCase(material)) {
                continue;
            }

            if (data.getAmount() != amount) {
                continue;
            }

            return entry;
        }

        for (ItemLogBatchEntry entry : batch.getEntries()) {
            if (entry == null || entry.getItemData() == null) {
                continue;
            }

            if (entry.getEntityUuid() != null) {
                continue;
            }

            LoggedItemData data = entry.getItemData();
            if (data.getMaterial().equalsIgnoreCase(material)) {
                return entry;
            }
        }

        return null;
    }

    public void markDestroyed(UUID entityUuid, ItemDestroyCause cause) {
        if (entityUuid == null) {
            return;
        }

        ItemLogBatch batch = trackedEntities.get(entityUuid);
        if (batch == null) {
            return;
        }

        LogTypeSettings settings = getSettingsForBatch(batch);
        if (settings == null || settings.getAggregationConfig() == null || !settings.getAggregationConfig().isTrackDestruction()) {
            return;
        }

        for (ItemLogBatchEntry entry : batch.getEntries()) {
            if (entityUuid.equals(entry.getEntityUuid())) {
                entry.markDestroyed(cause);
                break;
            }
        }
    }

    public void markPickedUp(UUID entityUuid) {
        if (entityUuid == null) {
            return;
        }

        ItemLogBatch batch = trackedEntities.get(entityUuid);
        if (batch == null) {
            return;
        }

        LogTypeSettings settings = getSettingsForBatch(batch);
        if (settings == null || settings.getAggregationConfig() == null || !settings.getAggregationConfig().isTrackPickups()) {
            return;
        }

        for (ItemLogBatchEntry entry : batch.getEntries()) {
            if (entityUuid.equals(entry.getEntityUuid())) {
                entry.markPickedUp();
                break;
            }
        }
    }

    private ItemLogBatch createBatch(Player player, String worldName, String coordinates, ItemLogBatchType type) {
        return new ItemLogBatch(
                UUID.randomUUID(),
                player.getUniqueId(),
                player.getName(),
                worldName,
                coordinates,
                type,
                System.currentTimeMillis()
        );
    }

    private void scheduleFlush(
            ItemLogBatch batch,
            LogTypeSettings settings,
            Map<UUID, ItemLogBatch> store,
            UUID playerUuid,
            long delayTicks
    ) {
        if (batch.getScheduledTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(batch.getScheduledTaskId());
        }

        long safeDelay = Math.max(1L, delayTicks);

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                flushBatch(batch, settings);
            } finally {
                store.remove(playerUuid);
                untrackBatchEntries(batch);
            }
        }, safeDelay).getTaskId();

        batch.setScheduledTaskId(taskId);
    }

    private void flushBatch(ItemLogBatch batch, LogTypeSettings settings) {
        if (batch == null || settings == null) {
            return;
        }

        if (batch.getType() != ItemLogBatchType.DEATH && batch.getEntries().isEmpty()) {
            return;
        }

        int maxShown = settings.getFilterConfig() == null ? 10 : settings.getFilterConfig().getMaxItemsShown();
        LogAggregationConfig aggregation = settings.getAggregationConfig();
        boolean includeDestroyed = aggregation != null && aggregation.isIncludeDestroyedItems();

        List<MergedItem> merged = BatchLogUtil.merge(batch.getEntries());

        switch (batch.getType()) {
            case DROP -> {
                String summary = BatchLogUtil.buildSummary(merged, maxShown);
                String destroyedSummary = includeDestroyed ? BatchLogUtil.buildDestroyedSummary(merged) : "";

                PlayerDropLogContext context = new PlayerDropLogContext(
                        batch.getPlayerName(),
                        batch.getWorldName(),
                        batch.getCoordinates(),
                        null,
                        "BATCH_DROP"
                );
                context.setCustomSummary(summary);
                context.setDestroyedItemsSummary(destroyedSummary);

                dispatcher.dispatchDrop(settings, context);
            }

            case PICKUP -> {
                String summary = BatchLogUtil.buildSummary(merged, maxShown);

                PlayerPickupLogContext context = new PlayerPickupLogContext(
                        batch.getPlayerName(),
                        batch.getWorldName(),
                        batch.getCoordinates(),
                        null,
                        "BATCH_PICKUP"
                );
                context.setCustomSummary(summary);

                dispatcher.dispatchPickup(settings, context);
            }

            case DEATH -> {
                String droppedSummary = merged.isEmpty() ? "Geen items gedropt." : BatchLogUtil.buildSummary(merged, maxShown);
                String destroyedSummary = includeDestroyed ? BatchLogUtil.buildDestroyedSummary(merged) : "";

                PlayerDeathLogContext context = new PlayerDeathLogContext(
                        batch.getPlayerName(),
                        batch.getWorldName(),
                        batch.getCoordinates(),
                        batch.getDeathCause(),
                        batch.getKillerName(),
                        batch.getXpSummary(),
                        batch.isKeepInventory(),
                        droppedSummary,
                        new ArrayList<>(),
                        destroyedSummary
                );

                dispatcher.dispatchDeath(settings, context);
            }
        }
    }

    private LogTypeSettings getSettingsForBatch(ItemLogBatch batch) {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null || batch == null) {
            return null;
        }

        return switch (batch.getType()) {
            case DROP -> plugin.getConfigManager().getLogsConfig().getPlayerDrops();
            case PICKUP -> plugin.getConfigManager().getLogsConfig().getPlayerPickups();
            case DEATH -> plugin.getConfigManager().getLogsConfig().getPlayerDeaths();
        };
    }

    private void untrackBatchEntries(ItemLogBatch batch) {
        if (batch == null || batch.getEntries() == null) {
            return;
        }

        for (ItemLogBatchEntry entry : batch.getEntries()) {
            if (entry == null || entry.getEntityUuid() == null) {
                continue;
            }

            trackedEntities.remove(entry.getEntityUuid());
        }
    }

    private void dispatchSingleDrop(Player player, LoggedItemData itemData, LogTypeSettings settings, String worldName, String coordinates) {
        PlayerDropLogContext context = new PlayerDropLogContext(
                player.getName(),
                worldName,
                coordinates,
                itemData,
                "DROP"
        );
        dispatcher.dispatchDrop(settings, context);
    }

    private void dispatchSinglePickup(Player player, LoggedItemData itemData, LogTypeSettings settings, String worldName, String coordinates) {
        PlayerPickupLogContext context = new PlayerPickupLogContext(
                player.getName(),
                worldName,
                coordinates,
                itemData,
                "GROUND_PICKUP"
        );
        dispatcher.dispatchPickup(settings, context);
    }

    private void dispatchSingleDeath(
            Player player,
            List<LoggedItemData> items,
            LogTypeSettings settings,
            String worldName,
            String coordinates,
            String cause,
            String killer,
            String xpSummary,
            boolean keepInventory
    ) {
        String summary = "Geen items gedropt.";

        if (items != null && !items.isEmpty()) {
            List<ItemLogBatchEntry> entries = new ArrayList<>();
            for (LoggedItemData item : items) {
                entries.add(new ItemLogBatchEntry(null, item));
            }

            List<MergedItem> merged = BatchLogUtil.merge(entries);
            int maxShown = settings.getFilterConfig() == null ? 10 : settings.getFilterConfig().getMaxItemsShown();
            summary = BatchLogUtil.buildSummary(merged, maxShown);
        }

        PlayerDeathLogContext context = new PlayerDeathLogContext(
                player.getName(),
                worldName,
                coordinates,
                cause,
                killer,
                xpSummary,
                keepInventory,
                summary,
                new ArrayList<>(),
                ""
        );

        dispatcher.dispatchDeath(settings, context);
    }
}