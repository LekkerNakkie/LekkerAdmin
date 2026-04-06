package me.lekkernakkie.lekkeradmin.service.freeze;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.model.freeze.FrozenPlayer;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftBotLogger;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftLogDeliveryMode;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftLogMessage;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftWebhookLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeService {

    private final LekkerAdmin plugin;
    private final Map<UUID, FrozenPlayer> frozenPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    private final MinecraftBotLogger botLogger;
    private final MinecraftWebhookLogger webhookLogger;

    public FreezeService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.botLogger = new MinecraftBotLogger(plugin);
        this.webhookLogger = new MinecraftWebhookLogger(plugin);
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("freeze.enabled", true);
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.containsKey(uuid);
    }

    public boolean isFrozen(Player player) {
        return isFrozen(player.getUniqueId());
    }

    public FrozenPlayer getFrozen(UUID uuid) {
        return frozenPlayers.get(uuid);
    }

    public FrozenPlayer getFrozen(Player player) {
        return getFrozen(player.getUniqueId());
    }

    public Collection<FrozenPlayer> getFrozenPlayers() {
        return Collections.unmodifiableCollection(frozenPlayers.values());
    }

    public boolean freeze(Player actor, Player target, String reason) {
        if (!isEnabled() || target == null) {
            return false;
        }

        if (isFrozen(target)) {
            return false;
        }

        UUID actorUuid = actor != null ? actor.getUniqueId() : null;
        String actorName = actor != null ? actor.getName() : "Console";

        FrozenPlayer frozenPlayer = new FrozenPlayer(
                target.getUniqueId(),
                target.getName(),
                actorUuid,
                actorName,
                reason == null || reason.isBlank() ? "/" : reason,
                System.currentTimeMillis()
        );

        frozenPlayers.put(target.getUniqueId(), frozenPlayer);

        if (shouldSendFreezeChat()) {
            sendFreezeMessages(target, frozenPlayer);
        }

        if (shouldShowBossBar()) {
            showBossBar(target);
        }

        logFreeze(target, frozenPlayer);
        plugin.debug("Player frozen: " + target.getName() + " by " + actorName + " reason=" + frozenPlayer.getReason());
        return true;
    }

    public boolean unfreeze(Player actor, Player target) {
        if (target == null) {
            return false;
        }

        FrozenPlayer removed = frozenPlayers.remove(target.getUniqueId());
        if (removed == null) {
            return false;
        }

        hideBossBar(target);

        if (shouldSendUnfreezeChat()) {
            target.sendMessage(plugin.lang().message(
                    "freeze.unfrozen-chat",
                    "{prefix} &aJe bent niet langer gefreezed."
            ));
        }

        logUnfreeze(target, removed, actor);
        plugin.debug("Player unfrozen: " + target.getName());
        return true;
    }

    public void restoreOnJoin(Player player) {
        if (!isFrozen(player)) {
            return;
        }

        if (shouldShowBossBar()) {
            showBossBar(player);
        }

        player.sendMessage(plugin.lang().message(
                "freeze.rejoin-still-frozen",
                "{prefix} &cJe bent nog steeds gefreezed."
        ));

        logRejoinRestore(player);
    }

    public void handleQuit(Player player) {
        if (!isFrozen(player)) {
            return;
        }

        hideBossBar(player);

        if (plugin.getConfig().getBoolean("freeze.quit.notify-staff", true)) {
            String msg = plugin.lang().format(
                    "freeze.quit-while-frozen",
                    "{prefix} &cSpeler &b{player} &cheeft de server verlaten tijdens freeze.",
                    Map.of("player", player.getName())
            );

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("lekkeradmin.freeze.notify") || p.hasPermission("lekkeradmin.admin"))
                    .forEach(p -> p.sendMessage(msg));
        }

        logQuitWhileFrozen(player);

        if (!plugin.getConfig().getBoolean("freeze.quit.keep-frozen-on-rejoin", true)) {
            frozenPlayers.remove(player.getUniqueId());
        }
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(bossBars.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                hideBossBar(player);
            }
        }
        bossBars.clear();
    }

    public boolean isMovementBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.movement", true);
    }

    public boolean isRotationOnlyAllowed() {
        return plugin.getConfig().getBoolean("freeze.block.rotation-only-allowed", true);
    }

    public boolean isCommandsBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.commands", true);
    }

    public boolean isDropBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.drop-items", true);
    }

    public boolean isPickupBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.pickup-items", true);
    }

    public boolean isInventoryBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.inventory-click", true);
    }

    public boolean isInteractBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.interact", true);
    }

    public boolean isBlockBreakBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.block-break", true);
    }

    public boolean isBlockPlaceBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.block-place", true);
    }

    public boolean isDamageDealBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.damage-deal", true);
    }

    public boolean isDamageTakeBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.damage-take", false);
    }

    public boolean isTeleportBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.teleport", true);
    }

    public boolean isCommandAllowed(String rawCommand) {
        List<String> whitelist = plugin.getConfig().getStringList("freeze.commands-whitelist");
        if (rawCommand == null || rawCommand.isBlank()) {
            return false;
        }

        String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        String base = command.split(" ")[0].toLowerCase(Locale.ROOT);

        for (String allowed : whitelist) {
            if (allowed != null && base.equalsIgnoreCase(allowed.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldSendFreezeChat() {
        return plugin.getConfig().getBoolean("freeze.messages.send-chat-on-freeze", true);
    }

    private boolean shouldSendUnfreezeChat() {
        return plugin.getConfig().getBoolean("freeze.messages.send-chat-on-unfreeze", true);
    }

    private boolean shouldShowBossBar() {
        return plugin.getConfig().getBoolean("freeze.bossbar.enabled", true)
                && plugin.getConfig().getBoolean("freeze.messages.send-bossbar-on-freeze", true);
    }

    private void sendFreezeMessages(Player target, FrozenPlayer frozenPlayer) {
        target.sendMessage(plugin.lang().message(
                "freeze.frozen-chat",
                "{prefix} &cJe bent gefreezed door staff."
        ));

        target.sendMessage(plugin.lang().format(
                "freeze.frozen-chat-reason",
                "{prefix} &7Reden: &f{reason}",
                Map.of("reason", frozenPlayer.getReason())
        ));

        target.sendMessage(plugin.lang().message(
                "freeze.frozen-chat-instructions",
                "{prefix} &7Blijf stilstaan en wacht op een stafflid."
        ));
    }

    private void showBossBar(Player player) {
        hideBossBar(player);

        String title = plugin.getConfig().getString("freeze.bossbar.title", "&cJe bent gefreezed &7- &fWacht op staff");
        String colorName = plugin.getConfig().getString("freeze.bossbar.color", "RED");
        String styleName = plugin.getConfig().getString("freeze.bossbar.style", "SOLID");
        float progress = (float) plugin.getConfig().getDouble("freeze.bossbar.progress", 1.0D);

        BossBar.Color color = parseColor(colorName);
        BossBar.Overlay overlay = parseOverlay(styleName);

        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(title);
        BossBar bossBar = BossBar.bossBar(component, Math.max(0F, Math.min(1F, progress)), color, overlay);

        player.showBossBar(bossBar);
        bossBars.put(player.getUniqueId(), bossBar);
    }

    private void hideBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private BossBar.Color parseColor(String name) {
        try {
            return BossBar.Color.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return BossBar.Color.RED;
        }
    }

    private BossBar.Overlay parseOverlay(String name) {
        try {
            return BossBar.Overlay.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private void logFreeze(Player target, FrozenPlayer frozenPlayer) {
        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getFreezeLogs();
        if (settings == null || !settings.isEnabled()) {
            return;
        }
        if (!plugin.getConfigManager().getLogsConfig().isFreezeLogFreezeEnabled()) {
            return;
        }

        String title = plugin.lang().get("freeze.log-freeze-title", "Freeze");
        String description = plugin.lang().format(
                "freeze.log-freeze-description",
                "{actor} heeft {player} gefreezed.",
                Map.of(
                        "actor", frozenPlayer.getActorName(),
                        "player", target.getName()
                )
        );

        sendLog(settings, buildLogMessage(
                settings,
                target,
                frozenPlayer.getActorName(),
                frozenPlayer.getReason(),
                "FREEZE",
                title,
                description,
                "#3498DB"
        ));
    }

    private void logUnfreeze(Player target, FrozenPlayer frozenPlayer, Player actor) {
        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getFreezeLogs();
        if (settings == null || !settings.isEnabled()) {
            return;
        }
        if (!plugin.getConfigManager().getLogsConfig().isFreezeLogUnfreezeEnabled()) {
            return;
        }

        String actorName = actor != null ? actor.getName() : "Console";
        String title = plugin.lang().get("freeze.log-unfreeze-title", "Unfreeze");
        String description = plugin.lang().format(
                "freeze.log-unfreeze-description",
                "{actor} heeft {player} ge-unfreezed.",
                Map.of(
                        "actor", actorName,
                        "player", target.getName()
                )
        );

        sendLog(settings, buildLogMessage(
                settings,
                target,
                actorName,
                frozenPlayer.getReason(),
                "UNFREEZE",
                title,
                description,
                "#2ECC71"
        ));
    }

    private void logQuitWhileFrozen(Player player) {
        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getFreezeLogs();
        if (settings == null || !settings.isEnabled()) {
            return;
        }
        if (!plugin.getConfig().getBoolean("freeze.quit.log", true)) {
            return;
        }
        if (!plugin.getConfigManager().getLogsConfig().isFreezeLogQuitEnabled()) {
            return;
        }

        FrozenPlayer frozen = getFrozen(player);
        String actorName = frozen != null ? frozen.getActorName() : "Onbekend";
        String reason = frozen != null ? frozen.getReason() : "/";

        String title = plugin.lang().get("freeze.log-quit-title", "Quit While Frozen");
        String description = plugin.lang().format(
                "freeze.log-quit-description",
                "{player} heeft de server verlaten terwijl hij gefreezed was.",
                Map.of("player", player.getName())
        );

        sendLog(settings, buildLogMessage(
                settings,
                player,
                actorName,
                reason,
                "QUIT_WHILE_FROZEN",
                title,
                description,
                "#E74C3C"
        ));
    }

    private void logRejoinRestore(Player player) {
        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getFreezeLogs();
        if (settings == null || !settings.isEnabled()) {
            return;
        }
        if (!plugin.getConfigManager().getLogsConfig().isFreezeLogRestoreEnabled()) {
            return;
        }

        FrozenPlayer frozen = getFrozen(player);
        String actorName = frozen != null ? frozen.getActorName() : "Onbekend";
        String reason = frozen != null ? frozen.getReason() : "/";

        String title = plugin.lang().get("freeze.log-restore-title", "Freeze Restored");
        String description = plugin.lang().format(
                "freeze.log-restore-description",
                "{player} kreeg zijn freeze-status terug bij relog.",
                Map.of("player", player.getName())
        );

        sendLog(settings, buildLogMessage(
                settings,
                player,
                actorName,
                reason,
                "REJOIN_RESTORE",
                title,
                description,
                "#9B59B6"
        ));
    }

    private MinecraftLogMessage buildLogMessage(LogTypeSettings settings,
                                                Player player,
                                                String actorName,
                                                String reason,
                                                String source,
                                                String title,
                                                String description,
                                                String fallbackColor) {

        if (!settings.isUseEmbeds()) {
            String plain = player.getName()
                    + " | " + title
                    + " | actor=" + actorName
                    + " | reden=" + reason
                    + " | wereld=" + player.getWorld().getName()
                    + " | xyz=" + formatLocation(player.getLocation())
                    + " | gamemode=" + prettyGamemode(player.getGameMode())
                    + " | source=" + source;
            return new MinecraftLogMessage(plain, null, null);
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);
        embed.setDescription(description);
        embed.setColor(parseJavaColor(settings.getEmbedConfig().getColor(), fallbackColor));
        embed.setFooter(settings.getEmbedConfig().getFooter());

        if (settings.getEmbedConfig().isUseTimestamp()) {
            embed.setTimestamp(Instant.now());
        }

        embed.addField(settings.getEmbedConfig().getFields().getPlayer(), player.getName(), true);
        embed.addField(settings.getEmbedConfig().getFields().getActor(), actorName, true);
        embed.addField(settings.getEmbedConfig().getFields().getReason(), reason, false);

        if (settings.getEmbedConfig().isShowWorld()) {
            embed.addField(settings.getEmbedConfig().getFields().getWorld(), player.getWorld().getName(), true);
        }

        if (settings.getEmbedConfig().isShowCoordinates()) {
            embed.addField(settings.getEmbedConfig().getFields().getCoordinates(), formatLocation(player.getLocation()), true);
        }

        embed.addField(settings.getEmbedConfig().getFields().getGamemode(), prettyGamemode(player.getGameMode()), true);
        embed.addField(settings.getEmbedConfig().getFields().getSource(), source, true);

        if (settings.getEmbedConfig().isShowPlayerHeadThumbnail()) {
            String uuid = player.getUniqueId().toString().replace("-", "");
            embed.setThumbnail("https://crafatar.com/avatars/" + uuid + "?overlay=true");
        }

        MessageEmbed built = embed.build();
        return new MinecraftLogMessage("", built, null);
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
            case NONE -> plugin.debug("Freeze log skipped: delivery mode NONE.");
        }
    }

    public String formatLocation(Location location) {
        if (location == null) {
            return "-";
        }
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    public String getGamemode(Player player) {
        GameMode gameMode = player.getGameMode();
        return gameMode != null ? gameMode.name() : "-";
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

    private Color parseJavaColor(String input, String fallback) {
        try {
            return Color.decode(input);
        } catch (Exception ex) {
            try {
                return Color.decode(fallback);
            } catch (Exception ignored) {
                return new Color(0x3498DB);
            }
        }
    }
}