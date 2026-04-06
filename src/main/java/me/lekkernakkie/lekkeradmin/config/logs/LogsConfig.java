package me.lekkernakkie.lekkeradmin.config.logs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class LogsConfig {

    private final FileConfiguration config;

    public LogsConfig(FileConfiguration config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }

    public String getTimezone() {
        return config.getString("timezone", "Europe/Brussels");
    }

    public LogTypeSettings getPlayerDrops() {
        return loadType("player-drops");
    }

    public LogTypeSettings getPlayerPickups() {
        return loadType("player-pickups");
    }

    public LogTypeSettings getPlayerDeaths() {
        return loadType("player-deaths");
    }

    public LogTypeSettings getPlayerJoins() {
        return loadType("player-joins");
    }

    public LogTypeSettings getPlayerLeaves() {
        return loadType("player-leaves");
    }

    public LogTypeSettings getInvseeLogs() {
        return loadType("invsee");
    }

    public LogTypeSettings getEnderchestLogs() {
        return loadType("enderchest");
    }

    public LogTypeSettings getPunishmentLogs() {
        return loadType("punishments");
    }

    public LogTypeSettings getHistoryClearLogs() {
        return loadType("history-clear");
    }

    public LogTypeSettings getExplosionLogs() {
        return loadType("explosion-logs");
    }

    public LogTypeSettings getVanishLogs() {
        return loadType("vanish");
    }

    public LogTypeSettings getFreezeLogs() {
        return loadType("freeze");
    }

    public boolean isVanishLogEnableEnabled() {
        return config.getBoolean("logs.vanish.flags.log-enable", true);
    }

    public boolean isVanishLogDisableEnabled() {
        return config.getBoolean("logs.vanish.flags.log-disable", true);
    }

    public boolean isVanishLogRestoreEnabled() {
        return config.getBoolean("logs.vanish.flags.log-rejoin-restore", true);
    }

    public boolean isFreezeLogFreezeEnabled() {
        return config.getBoolean("logs.freeze.flags.log-freeze", true);
    }

    public boolean isFreezeLogUnfreezeEnabled() {
        return config.getBoolean("logs.freeze.flags.log-unfreeze", true);
    }

    public boolean isFreezeLogQuitEnabled() {
        return config.getBoolean("logs.freeze.flags.log-quit-while-frozen", true);
    }

    public boolean isFreezeLogRestoreEnabled() {
        return config.getBoolean("logs.freeze.flags.log-rejoin-restore", true);
    }

    private LogTypeSettings loadType(String path) {
        String root = "logs." + path;
        String defaults = "defaults";

        boolean enabled = config.getBoolean(root + ".enabled", true);
        boolean useEmbeds = config.getBoolean(root + ".use-embeds", config.getBoolean(defaults + ".use-embeds", true));
        String deliveryMode = config.getString(root + ".delivery-mode", config.getString(defaults + ".delivery-mode", "BOT"));

        boolean logInvsee = config.getBoolean(root + ".log-invsee", true);
        boolean logEnderchest = config.getBoolean(root + ".log-enderchest", true);

        LogChannelConfig channelConfig = new LogChannelConfig(
                config.getString(root + ".bot.channel-id", config.getString(defaults + ".bot.channel-id", "")),
                config.getString(root + ".webhook.url", config.getString(defaults + ".webhook.url", "")),
                config.getString(root + ".webhook.username", config.getString(defaults + ".webhook.username", "LekkerAdmin Logs")),
                config.getString(root + ".webhook.avatar-url", config.getString(defaults + ".webhook.avatar-url", ""))
        );

        LogFilterConfig filterConfig = new LogFilterConfig(
                getStringListWithFallback(root + ".filters.ignored-worlds", defaults + ".ignored-worlds"),
                getStringListWithFallback(root + ".filters.ignored-materials", defaults + ".ignored-materials"),
                config.getBoolean(root + ".filters.only-log-if-enchanted", config.getBoolean(defaults + ".only-log-if-enchanted", false)),
                config.getInt(root + ".filters.min-amount", config.getInt(defaults + ".min-amount", 1)),
                config.getBoolean(root + ".filters.split-large-item-lists", config.getBoolean(defaults + ".split-large-item-lists", true)),
                config.getInt(root + ".filters.max-items-shown", config.getInt(defaults + ".max-items-shown", 10)),
                config.getBoolean(root + ".filters.deduplicate-death-drops", config.getBoolean(defaults + ".deduplicate-death-drops", true))
        );

        LogFieldConfig fieldConfig = new LogFieldConfig(
                getString(root + ".embed.fields.player", defaults + ".embed.fields.player", "Speler"),
                getString(root + ".embed.fields.actor", defaults + ".embed.fields.actor", "Staff"),
                getString(root + ".embed.fields.source", defaults + ".embed.fields.source", "Actie"),
                getString(root + ".embed.fields.world", defaults + ".embed.fields.world", "Wereld"),
                getString(root + ".embed.fields.coordinates", defaults + ".embed.fields.coordinates", "Coördinaten"),
                getString(root + ".embed.fields.item", defaults + ".embed.fields.item", "Items"),
                getString(root + ".embed.fields.amount", defaults + ".embed.fields.amount", "Aantal"),
                getString(root + ".embed.fields.item-name", defaults + ".embed.fields.item-name", "Item naam"),
                getString(root + ".embed.fields.enchantments", defaults + ".embed.fields.enchantments", "Enchantments"),
                getString(root + ".embed.fields.cause", defaults + ".embed.fields.cause", "Doodsoorzaak"),
                getString(root + ".embed.fields.killer", defaults + ".embed.fields.killer", "Killer"),
                getString(root + ".embed.fields.dropped-items", defaults + ".embed.fields.dropped-items", "Gedropte items"),
                getString(root + ".embed.fields.pickup-type", defaults + ".embed.fields.pickup-type", "Pickup type"),
                getString(root + ".embed.fields.drop-type", defaults + ".embed.fields.drop-type", "Drop type"),
                getString(root + ".embed.fields.xp", defaults + ".embed.fields.xp", "XP"),
                getString(root + ".embed.fields.keep-inventory", defaults + ".embed.fields.keep-inventory", "KeepInventory"),
                getString(root + ".embed.fields.reason", defaults + ".embed.fields.reason", "Reden"),
                getString(root + ".embed.fields.health", defaults + ".embed.fields.health", "Health"),
                getString(root + ".embed.fields.food", defaults + ".embed.fields.food", "Food"),
                getString(root + ".embed.fields.gamemode", defaults + ".embed.fields.gamemode", "Gamemode")
        );

        LogEmbedConfig embedConfig = new LogEmbedConfig(
                config.getString(root + ".embed.title", ""),
                config.getString(root + ".embed.description", ""),
                config.getString(root + ".embed.color", config.getString(defaults + ".embed.color", "#5865F2")),
                config.getString(root + ".embed.footer", config.getString(defaults + ".embed.footer", "")),
                config.getBoolean(root + ".embed.use-timestamp", config.getBoolean(defaults + ".embed.use-timestamp", true)),
                config.getBoolean(root + ".embed.show-player-head-thumbnail", config.getBoolean(defaults + ".show-player-head-thumbnail", true)),
                config.getBoolean(root + ".embed.show-enchantments", config.getBoolean(defaults + ".show-enchantments", true)),
                config.getBoolean(root + ".embed.show-lore", config.getBoolean(defaults + ".show-lore", false)),
                config.getBoolean(root + ".embed.show-world", config.getBoolean(defaults + ".show-world", true)),
                config.getBoolean(root + ".embed.show-coordinates", config.getBoolean(defaults + ".show-coordinates", true)),
                config.getInt(root + ".embed.max-field-length", config.getInt(defaults + ".max-field-length", 1024)),
                fieldConfig
        );

        LogAggregationConfig aggregationConfig = new LogAggregationConfig(
                config.getBoolean(root + ".aggregation.enabled", config.getBoolean(defaults + ".aggregation.enabled", true)),
                config.getInt(root + ".aggregation.flush-after-ticks", config.getInt(defaults + ".aggregation.flush-after-ticks", 40)),
                config.getBoolean(root + ".aggregation.track-destruction", config.getBoolean(defaults + ".aggregation.track-destruction", true)),
                config.getBoolean(root + ".aggregation.track-pickups", config.getBoolean(defaults + ".aggregation.track-pickups", true)),
                config.getBoolean(root + ".aggregation.include-destroyed-items", config.getBoolean(defaults + ".aggregation.include-destroyed-items", true))
        );

        return new LogTypeSettings(
                enabled,
                useEmbeds,
                deliveryMode,
                logInvsee,
                logEnderchest,
                channelConfig,
                filterConfig,
                embedConfig,
                aggregationConfig
        );
    }

    private String getString(String primaryPath, String fallbackPath, String fallback) {
        String value = config.getString(primaryPath);
        if (value != null) {
            return value;
        }

        value = config.getString(fallbackPath);
        if (value != null) {
            return value;
        }

        return fallback;
    }

    private List<String> getStringListWithFallback(String primaryPath, String fallbackPath) {
        List<String> primary = config.getStringList(primaryPath);
        if (primary != null && !primary.isEmpty()) {
            return primary;
        }

        List<String> fallback = config.getStringList(fallbackPath);
        return fallback == null ? new ArrayList<>() : fallback;
    }

    public List<String> getIgnoredWorldsFor(LogTypeSettings settings) {
        return settings == null ? new ArrayList<>() : settings.getFilterConfig().getIgnoredWorlds();
    }

    public List<String> getIgnoredMaterialsFor(LogTypeSettings settings) {
        return settings == null ? new ArrayList<>() : settings.getFilterConfig().getIgnoredMaterials();
    }

    public ConfigurationSection getRawSection(String path) {
        return config.getConfigurationSection(path);
    }
}