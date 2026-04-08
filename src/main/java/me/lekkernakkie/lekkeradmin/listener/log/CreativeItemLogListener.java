package me.lekkernakkie.lekkeradmin.listener.log;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.awt.Color;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CreativeItemLogListener implements Listener {

    private static final long SCAN_PERIOD_TICKS = 10L;
    private static final long IDLE_FLUSH_TICKS = 40L;

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
        registerProtocolListeners();
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

    private void registerProtocolListeners() {
        try {
            ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            if (manager == null) {
                plugin.debug("CreativeItemLogListener: ProtocolManager unavailable.");
                return;
            }

            registerPacketIfPresent(manager, "SET_CREATIVE_SLOT", "CREATIVE_SLOT_PACKET");
            registerPacketIfPresent(manager, "PICK_ITEM", "PICK_ITEM");
            registerPacketIfPresent(manager, "PICK_ITEM_FROM_BLOCK", "PICK_ITEM_FROM_BLOCK");
            registerPacketIfPresent(manager, "PICK_ITEM_FROM_ENTITY", "PICK_ITEM_FROM_ENTITY");
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Could not register ProtocolLib creative item listeners: " + throwable.getMessage());
        }
    }

    private void registerPacketIfPresent(ProtocolManager manager, String packetFieldName, String sourceName) {
        PacketType packetType = getClientPacketType(packetFieldName);
        if (packetType == null) {
            plugin.debug("CreativeItemLogListener: PacketType.Play.Client." + packetFieldName + " not available.");
            return;
        }

        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, packetType) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleTrackedPacket(event, sourceName);
            }
        });

        plugin.debug("CreativeItemLogListener: registered packet listener for " + packetFieldName + ".");
    }

    private PacketType getClientPacketType(String fieldName) {
        try {
            Field field = PacketType.Play.Client.class.getField(fieldName);
            Object value = field.get(null);
            if (value instanceof PacketType packetType) {
                return packetType;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void handleTrackedPacket(PacketEvent event, String sourceName) {
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        LogTypeSettings settings = getSettings();
        if (settings == null) {
            return;
        }

        if (isIgnoredWorld(player.getWorld().getName(), settings)) {
            return;
        }

        PlayerCreativeTracker tracker = trackers.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new PlayerCreativeTracker(player.getUniqueId(), snapshotInventory(player))
        );

        tracker.lastSource = sourceName;
        tracker.lastChangeAt = System.currentTimeMillis();
    }

    private void scanCreativePlayers() {
        LogTypeSettings settings = getSettings();
        if (settings == null) {
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
                    ignored -> new PlayerCreativeTracker(player.getUniqueId(), snapshotInventory(player))
            );

            Map<Material, Integer> current = snapshotInventory(player);
            ChangeSet changeSet = computeDiff(tracker.lastSnapshot, current, settings);

            if (!changeSet.spawned.isEmpty() || !changeSet.removed.isEmpty()) {
                mergeCounts(tracker.pendingSpawned, changeSet.spawned);
                mergeCounts(tracker.pendingRemoved, changeSet.removed);

                if ("UNKNOWN".equals(tracker.lastSource) || tracker.lastSource == null || tracker.lastSource.isBlank()) {
                    if (!changeSet.spawned.isEmpty() && changeSet.removed.isEmpty()) {
                        tracker.lastSource = "INSPAWNED";
                    } else if (changeSet.spawned.isEmpty() && !changeSet.removed.isEmpty()) {
                        tracker.lastSource = "REMOVED";
                    } else {
                        tracker.lastSource = "MIXED";
                    }
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
        return IDLE_FLUSH_TICKS * 50L;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ensureTracker(player, "CREATIVE_MENU");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ensureTracker(player, "CREATIVE_MENU");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ensureTracker(player, "HOTBAR_CHANGE");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ensureTracker(player, "SWAP_HANDS");
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
            mergeCounts(tracker.pendingSpawned, changeSet.spawned);
            mergeCounts(tracker.pendingRemoved, changeSet.removed);

            if ("UNKNOWN".equals(tracker.lastSource) || tracker.lastSource == null || tracker.lastSource.isBlank()) {
                if (!changeSet.spawned.isEmpty() && changeSet.removed.isEmpty()) {
                    tracker.lastSource = "INSPAWNED";
                } else if (changeSet.spawned.isEmpty() && !changeSet.removed.isEmpty()) {
                    tracker.lastSource = "REMOVED";
                } else {
                    tracker.lastSource = "MIXED";
                }
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
            mergeCounts(tracker.pendingSpawned, changeSet.spawned);
            mergeCounts(tracker.pendingRemoved, changeSet.removed);

            if ("UNKNOWN".equals(tracker.lastSource) || tracker.lastSource == null || tracker.lastSource.isBlank()) {
                if (!changeSet.spawned.isEmpty() && changeSet.removed.isEmpty()) {
                    tracker.lastSource = "INSPAWNED";
                } else if (changeSet.spawned.isEmpty() && !changeSet.removed.isEmpty()) {
                    tracker.lastSource = "REMOVED";
                } else {
                    tracker.lastSource = "MIXED";
                }
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
            ensureTracker(player, "JOIN_CREATIVE");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();

        if (event.getNewGameMode() == GameMode.CREATIVE) {
            Bukkit.getScheduler().runTask(plugin, () -> ensureTracker(player, "ENTER_CREATIVE"));
            return;
        }

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
            mergeCounts(tracker.pendingSpawned, changeSet.spawned);
            mergeCounts(tracker.pendingRemoved, changeSet.removed);

            if ("UNKNOWN".equals(tracker.lastSource) || tracker.lastSource == null || tracker.lastSource.isBlank()) {
                if (!changeSet.spawned.isEmpty() && changeSet.removed.isEmpty()) {
                    tracker.lastSource = "INSPAWNED";
                } else if (changeSet.spawned.isEmpty() && !changeSet.removed.isEmpty()) {
                    tracker.lastSource = "REMOVED";
                } else {
                    tracker.lastSource = "MIXED";
                }
            }
        }

        flushTracker(player, tracker, settings);
        trackers.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            stopScanner();
            trackers.clear();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        LogTypeSettings settings = getSettings();
        if (settings == null || !plugin.getConfigManager().getLogsConfig().isGiveCommandLogEnabled()) {
            return;
        }

        Player actor = event.getPlayer();
        GiveParseResult parsed = parsePlayerGiveCommand(actor, event.getMessage());
        if (parsed == null || parsed.target() == null) {
            return;
        }

        Player target = parsed.target();
        if (isIgnoredWorld(target.getWorld().getName(), settings) || isIgnoredMaterial(parsed.material(), settings)) {
            return;
        }

        scheduleGiveDeltaCapture(target, settings, "GIVE_COMMAND_PLAYER", "actor=" + actor.getName() + " | command=" + parsed.rawCommand());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        LogTypeSettings settings = getSettings();
        if (settings == null || !plugin.getConfigManager().getLogsConfig().isGiveCommandLogEnabled()) {
            return;
        }

        GiveParseResult parsed = parseServerGiveCommand(event.getSender(), event.getCommand());
        if (parsed == null || parsed.target() == null) {
            return;
        }

        Player target = parsed.target();
        if (isIgnoredWorld(target.getWorld().getName(), settings) || isIgnoredMaterial(parsed.material(), settings)) {
            return;
        }

        scheduleGiveDeltaCapture(target, settings, "GIVE_COMMAND_CONSOLE", "actor=" + parsed.actorName() + " | command=" + parsed.rawCommand());
    }

    private void scheduleGiveDeltaCapture(Player target, LogTypeSettings settings, String source, String details) {
        Map<Material, Integer> before = snapshotInventory(target);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isOnline()) {
                return;
            }

            Map<Material, Integer> after = snapshotInventory(target);
            ChangeSet diff = computeDiff(before, after, settings);

            if (diff.spawned.isEmpty() && diff.removed.isEmpty()) {
                return;
            }

            MinecraftLogMessage message = buildCreativeSummaryMessage(
                    settings,
                    target,
                    diff.spawned,
                    diff.removed,
                    source,
                    details
            );

            sendLog(settings, message);
        }, 2L);
    }

    private void ensureTracker(Player player, String source) {
        PlayerCreativeTracker tracker = trackers.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new PlayerCreativeTracker(player.getUniqueId(), snapshotInventory(player))
        );

        if (tracker.lastSource == null || tracker.lastSource.isBlank() || "UNKNOWN".equals(tracker.lastSource)) {
            tracker.lastSource = source;
        }
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
                tracker.lastSource,
                "creative inventory delta"
        );

        sendLog(settings, message);
        tracker.pendingSpawned.clear();
        tracker.pendingRemoved.clear();
        tracker.lastSource = "UNKNOWN";
        tracker.lastChangeAt = 0L;
    }

    private MinecraftLogMessage buildCreativeSummaryMessage(LogTypeSettings settings,
                                                            Player player,
                                                            Map<Material, Integer> spawned,
                                                            Map<Material, Integer> removed,
                                                            String source,
                                                            String details) {

        List<String> spawnedLines = toLines(spawned, "+");
        List<String> removedLines = toLines(removed, "-");

        String spawnedText = spawnedLines.isEmpty()
                ? "-"
                : joinLines(spawnedLines, settings.getEmbedConfig().getMaxFieldLength());

        String removedText = removedLines.isEmpty()
                ? "-"
                : joinLines(removedLines, settings.getEmbedConfig().getMaxFieldLength());

        String finalSource = source == null || source.isBlank() ? "UNKNOWN" : source;

        if (!settings.isUseEmbeds()) {
            String plain = player.getName()
                    + " | source=" + finalSource
                    + " | details=" + details
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
        embed.addField(settings.getEmbedConfig().getFields().getReason(), truncate(details, settings.getEmbedConfig().getMaxFieldLength()), false);

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

    private void mergeCounts(Map<Material, Integer> target, Map<Material, Integer> source) {
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

    private GiveParseResult parsePlayerGiveCommand(Player actor, String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return null;
        }

        String raw = rawMessage.startsWith("/") ? rawMessage.substring(1) : rawMessage;
        String[] parts = raw.split("\\s+");
        if (parts.length < 2) {
            return null;
        }

        String command = parts[0].toLowerCase(Locale.ROOT);
        if (!isGiveCommand(command)) {
            return null;
        }

        if (parts.length >= 3) {
            Player target = Bukkit.getPlayerExact(parts[1]);
            Material material = parseMaterial(parts[2]);

            if (target != null && material != null) {
                return new GiveParseResult(target, material, actor.getName(), raw);
            }
        }

        Material selfMaterial = parseMaterial(parts[1]);
        if (selfMaterial != null) {
            return new GiveParseResult(actor, selfMaterial, actor.getName(), raw);
        }

        return null;
    }

    private GiveParseResult parseServerGiveCommand(org.bukkit.command.CommandSender sender, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] parts = raw.split("\\s+");
        if (parts.length < 3) {
            return null;
        }

        String command = parts[0].toLowerCase(Locale.ROOT);
        if (!isGiveCommand(command)) {
            return null;
        }

        Player target = Bukkit.getPlayerExact(parts[1]);
        Material material = parseMaterial(parts[2]);

        if (target == null || material == null) {
            return null;
        }

        String actorName = sender != null ? sender.getName() : "Console";
        return new GiveParseResult(target, material, actorName, raw);
    }

    private boolean isGiveCommand(String command) {
        return command.equals("give") || command.equals("minecraft:give");
    }

    private Material parseMaterial(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String normalized = input.toUpperCase(Locale.ROOT);
        if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1);
        }

        try {
            return Material.valueOf(normalized);
        } catch (Exception ignored) {
            return null;
        }
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

    private String resolveTitle(LogTypeSettings settings, String fallback) {
        String title = settings.getEmbedConfig().getTitle();
        return title == null || title.isBlank() ? fallback : title;
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

    private String truncate(String value, int max) {
        if (value == null) {
            return "-";
        }

        if (max <= 0 || value.length() <= max) {
            return value;
        }

        return value.substring(0, max - 3) + "...";
    }

    private static final class PlayerCreativeTracker {
        private final UUID playerUuid;
        private Map<Material, Integer> lastSnapshot;
        private final Map<Material, Integer> pendingSpawned = new EnumMap<>(Material.class);
        private final Map<Material, Integer> pendingRemoved = new EnumMap<>(Material.class);
        private long lastChangeAt;
        private String lastSource = "UNKNOWN";

        private PlayerCreativeTracker(UUID playerUuid, Map<Material, Integer> lastSnapshot) {
            this.playerUuid = playerUuid;
            this.lastSnapshot = lastSnapshot;
        }
    }

    private record ChangeSet(Map<Material, Integer> spawned, Map<Material, Integer> removed) {
    }

    private record GiveParseResult(Player target, Material material, String actorName, String rawCommand) {
    }
}