package me.lekkernakkie.lekkeradmin.service.vanish;

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
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VanishService {

    private final LekkerAdmin plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Boolean> originalPickupState = new HashMap<>();

    private final MinecraftBotLogger botLogger;
    private final MinecraftWebhookLogger webhookLogger;

    public VanishService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.botLogger = new MinecraftBotLogger(plugin);
        this.webhookLogger = new MinecraftWebhookLogger(plugin);
    }

    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    public void enableVanish(Player player, boolean notify, String source) {
        boolean alreadyVanished = vanishedPlayers.contains(player.getUniqueId());
        vanishedPlayers.add(player.getUniqueId());

        if (!alreadyVanished) {
            originalPickupState.put(player.getUniqueId(), player.getCanPickupItems());
        }

        applyVanishState(player);

        if (notify) {
            player.sendMessage(plugin.lang().message(
                    "vanish.enabled",
                    "&7Vanish is nu &aingeschakeld&7."
            ));
        }

        logToggle(player, true, source);
    }

    public void disableVanish(Player player, boolean notify, String source) {
        vanishedPlayers.remove(player.getUniqueId());
        removeBossBar(player);
        restorePickupState(player);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(plugin, player);
        }

        if (notify) {
            player.sendMessage(plugin.lang().message(
                    "vanish.disabled",
                    "&7Vanish is nu &cuitgeschakeld&7."
            ));
        }

        logToggle(player, false, source);
    }

    public void handleJoin(Player joined) {
        for (UUID uuid : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null && vanished.isOnline()) {
                updateViewer(joined, vanished);
            }
        }

        if (isVanished(joined.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!joined.isOnline()) {
                    return;
                }

                if (!originalPickupState.containsKey(joined.getUniqueId())) {
                    originalPickupState.put(joined.getUniqueId(), joined.getCanPickupItems());
                }

                applyVanishState(joined);
                joined.sendMessage(plugin.lang().message(
                        "vanish.restored",
                        "&7Je vanish status werd opnieuw toegepast."
                ));
                logRestore(joined);
            }, 2L);
        }
    }

    public void handleQuit(Player player) {
        if (isVanished(player.getUniqueId())) {
            removeBossBar(player);
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isVanished(player.getUniqueId())) {
                restorePickupState(player);
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    viewer.showPlayer(plugin, player);
                }
            }
        }

        for (BossBar bossBar : bossBars.values()) {
            bossBar.removeAll();
        }
        bossBars.clear();
        vanishedPlayers.clear();
        originalPickupState.clear();
    }

    private void applyVanishState(Player player) {
        if (plugin.getConfigManager().getMainConfig().isVanishBlockItemPickup()) {
            player.setCanPickupItems(false);
        }

        if (plugin.getConfigManager().getMainConfig().isVanishBossBarEnabled()) {
            showBossBar(player);
        } else {
            removeBossBar(player);
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            updateViewer(viewer, player);
        }
    }

    private void updateViewer(Player viewer, Player vanished) {
        if (viewer.getUniqueId().equals(vanished.getUniqueId())) {
            return;
        }

        if (canSeeVanished(viewer)) {
            viewer.showPlayer(plugin, vanished);
        } else {
            viewer.hidePlayer(plugin, vanished);
        }
    }

    private boolean canSeeVanished(Player viewer) {
        return viewer.hasPermission("lekkeradmin.admin")
                || viewer.hasPermission("lekkeradmin.vanish.see");
    }

    private void restorePickupState(Player player) {
        Boolean previous = originalPickupState.remove(player.getUniqueId());
        if (previous != null) {
            player.setCanPickupItems(previous);
            return;
        }

        if (plugin.getConfigManager().getMainConfig().isVanishBlockItemPickup()) {
            player.setCanPickupItems(true);
        }
    }

    private void showBossBar(Player player) {
        removeBossBar(player);

        String title = plugin.lang().get(
                "vanish.bossbar",
                "&dJe bent momenteel in vanish"
        );

        BossBar bossBar = Bukkit.createBossBar(
                title,
                parseBarColor(plugin.getConfigManager().getMainConfig().getVanishBossBarColor()),
                parseBarStyle(plugin.getConfigManager().getMainConfig().getVanishBossBarStyle())
        );

        bossBar.setProgress(1.0D);
        bossBar.addPlayer(player);
        bossBars.put(player.getUniqueId(), bossBar);
    }

    private void removeBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    private void logRestore(Player player) {
        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getVanishLogs();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        sendLog(settings, buildLogMessage(
                settings,
                player,
                plugin.lang().get("vanish.log-restore-title", "Vanish Restored"),
                plugin.lang().format(
                        "vanish.log-restore-description",
                        "{player} kreeg vanish opnieuw bij relog.",
                        Map.of("player", player.getName())
                ),
                "#9B59B6",
                "RESTORE"
        ));
    }

    private void logToggle(Player player, boolean enabled, String source) {
        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getVanishLogs();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        if (enabled && !plugin.getConfigManager().getLogsConfig().isVanishLogEnableEnabled()) {
            return;
        }

        if (!enabled && !plugin.getConfigManager().getLogsConfig().isVanishLogDisableEnabled()) {
            return;
        }

        String title = enabled
                ? plugin.lang().get("vanish.log-enabled-title", "Vanish Enabled")
                : plugin.lang().get("vanish.log-disabled-title", "Vanish Disabled");

        String description = enabled
                ? plugin.lang().format(
                "vanish.log-enabled-description",
                "{player} heeft vanish ingeschakeld.",
                Map.of("player", player.getName())
        )
                : plugin.lang().format(
                "vanish.log-disabled-description",
                "{player} heeft vanish uitgeschakeld.",
                Map.of("player", player.getName())
        );

        String color = enabled ? "#2ECC71" : "#E74C3C";
        sendLog(settings, buildLogMessage(settings, player, title, description, color, source));
    }

    private MinecraftLogMessage buildLogMessage(LogTypeSettings settings,
                                                Player player,
                                                String title,
                                                String description,
                                                String fallbackColor,
                                                String source) {

        if (!settings.isUseEmbeds()) {
            String plain = player.getName()
                    + " | "
                    + title
                    + " | wereld="
                    + player.getWorld().getName()
                    + " | xyz="
                    + formatLocation(player.getLocation())
                    + " | gamemode="
                    + prettyGamemode(player.getGameMode())
                    + " | source="
                    + source;
            return new MinecraftLogMessage(plain, null, null);
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);
        embed.setDescription(description);
        embed.setColor(parseColor(settings.getEmbedConfig().getColor(), fallbackColor));
        embed.setFooter(settings.getEmbedConfig().getFooter());
        if (settings.getEmbedConfig().isUseTimestamp()) {
            embed.setTimestamp(Instant.now());
        }

        String playerField = settings.getEmbedConfig().getFields().getPlayer();
        String worldField = settings.getEmbedConfig().getFields().getWorld();
        String coordinatesField = settings.getEmbedConfig().getFields().getCoordinates();
        String gamemodeField = settings.getEmbedConfig().getFields().getGamemode();
        String reasonField = settings.getEmbedConfig().getFields().getReason();

        embed.addField(playerField, player.getName(), true);

        if (settings.getEmbedConfig().isShowWorld()) {
            embed.addField(worldField, player.getWorld().getName(), true);
        }

        if (settings.getEmbedConfig().isShowCoordinates()) {
            embed.addField(coordinatesField, formatLocation(player.getLocation()), true);
        }

        embed.addField(gamemodeField, prettyGamemode(player.getGameMode()), true);
        embed.addField(reasonField, source, true);

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
            case NONE -> plugin.debug("Vanish log skipped: delivery mode NONE.");
        }
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

    private BarColor parseBarColor(String input) {
        try {
            return BarColor.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return BarColor.PURPLE;
        }
    }

    private BarStyle parseBarStyle(String input) {
        try {
            return BarStyle.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return BarStyle.SOLID;
        }
    }

    private Color parseColor(String input, String fallback) {
        try {
            return Color.decode(input);
        } catch (Exception ex) {
            try {
                return Color.decode(fallback);
            } catch (Exception ignored) {
                return new Color(0x5865F2);
            }
        }
    }
}