package me.lekkernakkie.lekkeradmin.listener.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftBotLogger;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftLogDeliveryMode;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftLogMessage;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftWebhookLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CreativeItemLogListener implements Listener {

    private static final long SCAN_PERIOD_TICKS = 10L;      // 0.5s
    private static final long IDLE_FLUSH_TICKS = 40L;       // 2s zonder veranderingen

    private final LekkerAdmin plugin;
    private final MinecraftBotLogger botLogger;
    private final MinecraftWebhookLogger webhookLogger;

    private final Map<UUID, PlayerCreativeTracker> trackers = new HashMap<>();
    private BukkitTask scanTask;

    public CreativeItemLogListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.botLogger = new MinecraftBotLogger(plugin);
        this.webhookLogger = new MinecraftWebhookLogger(plugin);
        startScanner();
    }

    private void startScanner() {
        stopScanner();
        this.scanTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::scanCreativePlayers,
                20L,
                SCAN_PERIOD_TICKS
        );
    }

    private void stopScanner() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    private void scanCreativePlayers() {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return;
        }

        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getCreativeItemsLogs();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                continue;
            }

            if (isIgnoredWorld(player.getWorld().getName(), settings)) {
                continue;
            }

            PlayerCreativeTracker tracker = trackers.computeIfAbsent(
                    player.getUniqueId(),
                    ignored -> new PlayerCreativeTracker(
                            player.getUniqueId(),
                            snapshotInventory(player)
                    )
            );

            Map<Material, Integer> current = snapshotInventory(player);
            ChangeSet changeSet = computeDiff(tracker.lastSnapshot, current, settings);

            if (!changeSet.spawned.isEmpty() || !changeSet.removed.isEmpty()) {
                mergeLines(tracker.pendingSpawned, changeSet.spawned);
                mergeLines(tracker.pendingRemoved, changeSet.removed);

                if (!changeSet.spawned.isEmpty() && changeSet.removed.isEmpty()) {
                    tracker.lastSource = "INSPAWNED";
                } else if (changeSet.spawned.isEmpty() && !changeSet.removed.isEmpty()) {
                    tracker.lastSource = "REMOVED";
                } else {
                    tracker.lastSource = "MIXED";
                }

                tracker.lastChangeAt = now;
                tracker.lastSnapshot = current;
                continue;
            }

            tracker.lastSnapshot = current;

            if ((!tracker.pendingSpawned.isEmpty() || !tracker.pendingRemoved.isEmpty())
                    && now - tracker.lastChangeAt >= idleMillis()) {
                flushTracker(player, tracker, settings);
            }
        }
    }

    private long idleMillis() {
        return (IDLE_FLUSH_TICKS * 50L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ensureTracker(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ensureTracker(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ensureTracker(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ensureTracker(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        LogTypeSettings settings = getSettings();
        if (settings == null) {
            return;
        }

        PlayerCreativeTracker tracker = trackers.get(player.getUniqueId());
        if (tracker == null) {
            return;
        }

        Map<Material, Integer> current = snapshotInventory(player);
        ChangeSet changeSet = computeDiff(tracker.lastSnapshot, current, settings);

        if (!changeSet.spawned.isEmpty() || !changeSet.removed.isEmpty()) {
            mergeLines(tracker.pendingSpawned, changeSet.spawned);
            mergeLines(tracker.pendingRemoved, changeSet.removed);

            if (!changeSet.spawned.isEmpty() && changeSet.removed.isEmpty()) {
                tracker.lastSource = "INSPAWNED";
            } else if (changeSet.spawned.isEmpty() && !changeSet.removed.isEmpty()) {
                tracker.lastSource = "REMOVED";
            } else {
                tracker.lastSource = "MIXED";
            }

            tracker.lastSnapshot = current;
            tracker.lastChangeAt = System.currentTimeMillis();
        }

        flushTracker(player, tracker, settings);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        LogTypeSettings settings = getSettings();
        if (settings == null) {
            trackers.remove(player.getUniqueId());
            return;
        }

        PlayerCreativeTracker tracker = trackers.get(player.getUniqueId());
        if (tracker == null) {
            return;
        }

        Map<Material, Integer> current = snapshotInventory(player);
        ChangeSet changeSet = computeDiff(tracker.lastSnapshot, current, settings);

        if (!changeSet.spawned.isEmpty() || !changeSet.removed.isEmpty()) {
            mergeLines(tracker.pendingSpawned, changeSet.spawned);
            mergeLines(tracker.pendingRemoved, changeSet.removed);

            if (!changeSet.spawned.isEmpty() && changeSet.removed.isEmpty()) {
                tracker.lastSource = "INSPAWNED";
            } else if (changeSet.spawned.isEmpty() && !changeSet.removed.isEmpty()) {
                tracker.lastSource = "REMOVED";
            } else {
                tracker.lastSource = "MIXED";
            }

            tracker.lastSnapshot = current;
        }

        flushTracker(player, tracker, settings);
        trackers.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            ensureTracker(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();

        if (event.getNewGameMode() == GameMode.CREATIVE) {
            Bukkit.getScheduler().runTask(plugin, () -> ensureTracker(player));
            return;
        }

        LogTypeSettings settings = getSettings();
        if (settings == null) {
            trackers.remove(player.getUniqueId());
            return;
        }

        PlayerCreativeTracker tracker = trackers.get(player.getUniqueId());
        if (tracker != null) {
            Map<Material, Integer> current = snapshotInventory(player);
            ChangeSet changeSet = computeDiff(tracker.lastSnapshot, current, settings);

            if (!changeSet.spawned.isEmpty() || !changeSet.removed.isEmpty()) {
                mergeLines(tracker.pendingSpawned, changeSet.spawned);
                mergeLines(tracker.pendingRemoved, changeSet.removed);

                if (!changeSet.spawned.isEmpty() && changeSet.removed.isEmpty()) {
                    tracker.lastSource = "INSPAWNED";
                } else if (changeSet.spawned.isEmpty() && !changeSet.removed.isEmpty()) {
                    tracker.lastSource = "REMOVED";
                } else {
                    tracker.lastSource = "MIXED";
                }
            }

            flushTracker(player, tracker, settings);
            trackers.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            stopScanner();
            trackers.clear();
        }
    }

    private void ensureTracker(Player player) {
        trackers.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new PlayerCreativeTracker(
                        player.getUniqueId(),
                        snapshotInventory(player)
                )
        );
    }

    private LogTypeSettings getSettings() {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return null;
        }

        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getCreativeItemsLogs();
        if (settings == null || !settings.isEnabled()) {
            return null;
        }

        return settings;
    }

    private void flushTracker(Player player, PlayerCreativeTracker tracker, LogTypeSettings settings) {
        if (tracker.pendingSpawned.isEmpty() && tracker.pendingRemoved.isEmpty()) {
            return;
        }

        MinecraftLogMessage message = buildCreativeSummaryMessage(
                settings,
                player,
                tracker.pendingSpawned,
                tracker.pendingRemoved,
                tracker.lastSource
        );

        sendLog(settings, message);
        tracker.pendingSpawned.clear();
        tracker.pendingRemoved.clear();
        tracker.lastSource = "MIXED";
        tracker.lastChangeAt = 0L;
    }

    private MinecraftLogMessage buildCreativeSummaryMessage(LogTypeSettings settings,
                                                            Player player,
                                                            Map<Material, Integer> spawned,
                                                            Map<Material, Integer> removed,
                                                            String source) {

        List<String> spawnedLines = toLines(spawned, "+");
        List<String> removedLines = toLines(removed, "-");

        String spawnedText = spawnedLines.isEmpty()
                ? "-"
                : joinLines(spawnedLines, settings.getEmbedConfig().getMaxFieldLength());

        String removedText = removedLines.isEmpty()
                ? "-"
                : joinLines(removedLines, settings.getEmbedConfig().getMaxFieldLength());

        String finalSource = source == null || source.isBlank() ? "MIXED" : source;

        if (!settings.isUseEmbeds()) {
            String plain = player.getName()
                    + " | source=" + finalSource
                    + " | spawned=" + spawnedText.replace("\n", " | ")
                    + " | removed=" + removedText.replace("\n", " | ")
                    + " | world=" + player.getWorld().getName()
                    + " | xyz=" + formatLocation(player.getLocation())
                    + " | gamemode=" + prettyGamemode(player.getGameMode());

            return new MinecraftLogMessage(plain, null, null);
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(resolveTitle(settings, "Creative Items"));
        embed.setDescription(player.getName() + " verkreeg of verwijderde items buiten survival.");
        embed.setColor(parseColor(settings.getEmbedConfig().getColor(), "#F1C40F"));
        embed.setFooter(settings.getEmbedConfig().getFooter());

        if (settings.getEmbedConfig().isUseTimestamp()) {
            embed.setTimestamp(Instant.now());
        }

        embed.addField(settings.getEmbedConfig().getFields().getPlayer(), player.getName(), true);

        if (settings.getEmbedConfig().isShowWorld()) {
            embed.addField(settings.getEmbedConfig().getFields().getWorld(), player.getWorld().getName(), true);
        }

        if (settings.getEmbedConfig().isShowCoordinates()) {
            embed.addField(settings.getEmbedConfig().getFields().getCoordinates(), formatLocation(player.getLocation()), true);
        }

        embed.addField(settings.getEmbedConfig().getFields().getGamemode(), prettyGamemode(player.getGameMode()), true);
        embed.addField(settings.getEmbedConfig().getFields().getSource(), finalSource, true);
        embed.addField("Ingespawned", spawnedText, false);
        embed.addField("Terug weg / verminderd", removedText, false);

        if (settings.getEmbedConfig().isShowPlayerHeadThumbnail()) {
            embed.setThumbnail("https://mc-heads.net/avatar/" + player.getUniqueId());
        }

        MessageEmbed built = embed.build();
        return new MinecraftLogMessage("", built, null);
    }

    private ChangeSet computeDiff(Map<Material, Integer> before,
                                  Map<Material, Integer> after,
                                  LogTypeSettings settings) {

        Map<Material, Integer> spawned = new EnumMap<>(Material.class);
        Map<Material, Integer> removed = new EnumMap<>(Material.class);

        for (Material material : Material.values()) {
            if (material == null || material == Material.AIR || !material.isItem()) {
                continue;
            }

            if (isIgnoredMaterial(material, settings)) {
                continue;
            }

            int beforeAmount = before.getOrDefault(material, 0);
            int afterAmount = after.getOrDefault(material, 0);
            int diff = afterAmount - beforeAmount;

            if (diff > 0) {
                spawned.put(material, diff);
            } else if (diff < 0) {
                removed.put(material, Math.abs(diff));
            }
        }

        return new ChangeSet(spawned, removed);
    }

    private Map<Material, Integer> snapshotInventory(Player player) {
        Map<Material, Integer> counts = new EnumMap<>(Material.class);

        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            counts.merge(stack.getType(), stack.getAmount(), Integer::sum);
        }

        return counts;
    }

    private void mergeLines(Map<Material, Integer> target, Map<Material, Integer> source) {
        for (Map.Entry<Material, Integer> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    private List<String> toLines(Map<Material, Integer> items, String prefix) {
        List<String> lines = new ArrayList<>();

        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            lines.add(prefix + entry.getValue() + " " + entry.getKey().name());
        }

        return lines;
    }

    private String resolveTitle(LogTypeSettings settings, String fallback) {
        String title = settings.getEmbedConfig().getTitle();
        return title == null || title.isBlank() ? fallback : title;
    }

    private boolean isIgnoredWorld(String worldName, LogTypeSettings settings) {
        if (worldName == null || settings.getFilterConfig() == null || settings.getFilterConfig().getIgnoredWorlds() == null) {
            return false;
        }

        return settings.getFilterConfig().getIgnoredWorlds().stream()
                .anyMatch(world -> world != null && world.equalsIgnoreCase(worldName));
    }

    private boolean isIgnoredMaterial(Material material, LogTypeSettings settings) {
        if (material == null || settings.getFilterConfig() == null || settings.getFilterConfig().getIgnoredMaterials() == null) {
            return false;
        }

        return settings.getFilterConfig().getIgnoredMaterials().stream()
                .anyMatch(name -> name != null && name.equalsIgnoreCase(material.name()));
    }

    private void sendLog(LogTypeSettings settings, MinecraftLogMessage message) {
        MinecraftLogDeliveryMode mode = MinecraftLogDeliveryMode.fromString(settings.getDeliveryMode());

        switch (mode) {
            case BOT -> botLogger.send(settings, message);
            case WEBHOOK -> webhookLogger.send(settings, message);
            case BOTH -> {
                botLogger.send(settings, message);
                webhookLogger.send(settings, message);
            }
            case NONE -> plugin.debug("Creative item log skipped: delivery mode NONE.");
        }
    }

    private String joinLines(List<String> lines, int maxLength) {
        StringBuilder builder = new StringBuilder();

        for (String line : lines) {
            if (builder.length() == 0) {
                builder.append(line);
                continue;
            }

            if (builder.length() + 1 + line.length() > maxLength) {
                builder.append("\n...");
                break;
            }

            builder.append('\n').append(line);
        }

        return builder.toString();
    }

    private String formatLocation(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private String prettyGamemode(GameMode gameMode) {
        String lower = gameMode.name().toLowerCase(Locale.ROOT);
        String[] parts = lower.split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }

        return builder.toString();
    }

    private Color parseColor(String input, String fallback) {
        try {
            return Color.decode(input);
        } catch (Exception ex) {
            try {
                return Color.decode(fallback);
            } catch (Exception ignored) {
                return new Color(0xF1C40F);
            }
        }
    }

    private static final class PlayerCreativeTracker {
        private final UUID playerUuid;
        private Map<Material, Integer> lastSnapshot;
        private final Map<Material, Integer> pendingSpawned = new EnumMap<>(Material.class);
        private final Map<Material, Integer> pendingRemoved = new EnumMap<>(Material.class);
        private long lastChangeAt;
        private String lastSource = "MIXED";

        private PlayerCreativeTracker(UUID playerUuid, Map<Material, Integer> lastSnapshot) {
            this.playerUuid = playerUuid;
            this.lastSnapshot = lastSnapshot;
        }
    }

    private record ChangeSet(Map<Material, Integer> spawned, Map<Material, Integer> removed) {
    }
}