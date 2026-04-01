package me.lekkernakkie.lekkeradmin.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

public class PunishmentsConfig {

    private final FileConfiguration config;

    public PunishmentsConfig(FileConfiguration config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.getBoolean("settings.enabled", true);
    }

    public String getTimezone() {
        return config.getString("settings.timezone", "Europe/Brussels");
    }

    public String getDateFormat() {
        return config.getString("settings.date-format", "dd-MM-yyyy HH:mm:ss");
    }

    public String getServerName() {
        return config.getString("settings.server-name", "Sakurahelm");
    }

    public List<String> getPermanentKeywords() {
        return config.getStringList("settings.permanent-keywords");
    }

    public boolean isOfflineNotificationsEnabled() {
        return config.getBoolean("settings.offline-notifications", true);
    }

    public boolean isSelfPunishmentAllowed() {
        return config.getBoolean("settings.allow-self-punishment", false);
    }

    public int getExpiryCheckSeconds() {
        return Math.max(1, config.getInt("settings.expiry-check-seconds", 5));
    }

    public String getDefaultReason(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "warn" -> config.getString("defaults.warn-reason", "Geen reden opgegeven.");
            case "ban" -> config.getString("defaults.ban-reason", "Geen reden opgegeven.");
            case "unban" -> config.getString("defaults.unban-reason", "Geen reden opgegeven.");
            case "mute" -> config.getString("defaults.mute-reason", "Geen reden opgegeven.");
            case "unmute" -> config.getString("defaults.unmute-reason", "Geen reden opgegeven.");
            case "kick" -> config.getString("defaults.kick-reason", "Geen reden opgegeven.");
            default -> "Geen reden opgegeven.";
        };
    }

    public int getBanlistEntriesPerPage() {
        return config.getInt("banlist.entries-per-page", 10);
    }

    public boolean isDiscordDMEnabled() {
        return config.getBoolean("discord-dm.enabled", true);
    }

    public boolean isDiscordDMEnabled(String type) {
        return config.getBoolean("discord-dm." + type + ".enabled", true);
    }

    public String getDiscordTitle(String type) {
        return config.getString("discord-dm." + type + ".title", "Moderatie");
    }

    public int getDiscordDmColor(String type) {
        return parseColor("discord-dm." + type + ".color", "#7289DA");
    }

    public int getBanColor() {
        return parseColor("discord-logging.colors.ban", "#FF4D4D");
    }

    public int getUnbanColor() {
        return parseColor("discord-logging.colors.unban", "#33CC66");
    }

    public int getMuteColor() {
        return parseColor("discord-logging.colors.mute", "#FFB347");
    }

    public int getUnmuteColor() {
        return parseColor("discord-logging.colors.unmute", "#66CCFF");
    }

    public int getKickColor() {
        return parseColor("discord-logging.colors.kick", "#F39C12");
    }

    public int getWarnColor() {
        return parseColor("discord-logging.colors.warn", "#F1C40F");
    }

    public boolean isPlayerInfoEnabled() {
        return config.getBoolean("playerinfo.enabled", true);
    }

    public String getPlayerInfoRequiredRoleId() {
        return config.getString("playerinfo.required-role-id", "");
    }

    public String getPlayerInfoSkinHeadUrl() {
        return config.getString("playerinfo.skin-head-url", "https://mc-heads.net/avatar/{minecraft_name}/256");
    }

    public int getPlayerInfoActiveLimit() {
        return config.getInt("playerinfo.active-limit", 5);
    }

    public int getPlayerInfoExpiredLimit() {
        return config.getInt("playerinfo.expired-limit", 5);
    }

    public String getPlayerInfoNoResultsMessage() {
        return config.getString("playerinfo.no-results-message", "Geen speler gevonden.");
    }

    public String getPlayerInfoUnknownValue() {
        return config.getString("playerinfo.unknown-value", "Onbekend");
    }

    private int parseColor(String path, String fallbackHex) {
        String value = config.getString(path, fallbackHex);
        try {
            return Color.decode(value).getRGB() & 0xFFFFFF;
        } catch (Exception ex) {
            return Color.decode(fallbackHex).getRGB() & 0xFFFFFF;
        }
    }
}