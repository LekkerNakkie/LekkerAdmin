package me.lekkernakkie.lekkeradmin.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.awt.Color;
import java.util.List;

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

    public String getNoPermissionMessage() {
        return config.getString("messages.no-permission", "&cDaar edde gij het lef ni vur..");
    }

    public String getPlayerNotFoundMessage() {
        return config.getString("messages.player-not-found", "&cSpeler niet gevonden.");
    }

    public String getPlayerNameRequiredMessage() {
        return config.getString("messages.player-name-required", "&cJe moet een speler opgeven.");
    }

    public String getReasonRequiredMessage() {
        return config.getString("messages.reason-required", "&cJe moet een reden opgeven.");
    }

    public String getDurationRequiredMessage() {
        return config.getString("messages.duration-required", "&cJe moet een tijd of &eperm &copgeven.");
    }

    public String getInvalidDurationMessage() {
        return config.getString("messages.invalid-duration", "&cOngeldige tijd. Gebruik bv: &710m, 1h, 7d, 1h30m, perm");
    }

    public String getAlreadyBannedMessage() {
        return config.getString("messages.already-banned", "&cDeze speler heeft al een actieve ban.");
    }

    public String getNotBannedMessage() {
        return config.getString("messages.not-banned", "&cDeze speler heeft geen actieve ban.");
    }

    public String getAlreadyMutedMessage() {
        return config.getString("messages.already-muted", "&cDeze speler heeft al een actieve mute.");
    }

    public String getNotMutedMessage() {
        return config.getString("messages.not-muted", "&cDeze speler heeft geen actieve mute.");
    }

    public String getCannotPunishSelfMessage() {
        return config.getString("messages.cannot-punish-self", "&cJe kan jezelf niet straffen.");
    }

    public String getUsageBanMessage() {
        return config.getString("messages.usage-ban", "&eGebruik: &7/ban <speler> <tijd|perm> [reden]");
    }

    public String getUsageUnbanMessage() {
        return config.getString("messages.usage-unban", "&eGebruik: &7/unban <speler> [reden]");
    }

    public String getUsageMuteMessage() {
        return config.getString("messages.usage-mute", "&eGebruik: &7/mute <speler> <tijd|perm> [reden]");
    }

    public String getUsageUnmuteMessage() {
        return config.getString("messages.usage-unmute", "&eGebruik: &7/unmute <speler> [reden]");
    }

    public String getUsageKickMessage() {
        return config.getString("messages.usage-kick", "&eGebruik: &7/kick <speler> [reden]");
    }

    public String getUsageWarnMessage() {
        return config.getString("messages.usage-warn", "&eGebruik: &7/warn <speler> [reden]");
    }

    public String getUsageBanlistMessage() {
        return config.getString("messages.usage-banlist", "&eGebruik: &7/banlist [pagina]");
    }

    public String getCommandOnlyForPlayersMessage() {
        return config.getString("messages.command-only-for-players", "&cDit commando kan enkel door spelers gebruikt worden.");
    }

    public String getHistoryUsageMessage() {
        return config.getString("history.usage", "&eGebruik: &7/history <speler> [pagina]");
    }

    public String getHistoryInvalidPageMessage() {
        return config.getString("history.invalid-page", "&cPagina moet een nummer zijn.");
    }

    public String getHistoryPageTooLowMessage() {
        return config.getString("history.page-too-low", "&cPagina moet groter zijn dan 0.");
    }

    public String getHistoryPageNotFoundMessage() {
        return config.getString("history.page-not-found", "&cDie pagina bestaat niet. Max pagina: &f{max_page}&c.");
    }

    public String getHistoryEmptyMessage() {
        return config.getString("history.empty", "&cGeen punishment history gevonden voor &f{player}&c.");
    }

    public String getHistoryHeaderLine() {
        return config.getString("history.header-line", "&8&m------------------------------------------------");
    }

    public String getHistoryTitle() {
        return config.getString("history.title", "&cPunishment History &8» &f{player} &8(&7Pagina &f{page}&7/&f{max_page}&8)");
    }

    public String getHistoryEntryHeader() {
        return config.getString("history.entry-header", "&6#{index} &8| &7ID: &f{id} &8| &7Type: &f{type}{status_part}");
    }

    public String getHistoryEntryReason() {
        return config.getString("history.entry-reason", "&8  &7Reden: &f{reason}");
    }

    public String getHistoryEntryIssued() {
        return config.getString("history.entry-issued", "&8  &7Uitgedeeld door: &f{actor} &8| &7Op: &f{issued_at}");
    }

    public String getHistoryEntryDuration() {
        return config.getString("history.entry-duration", "&8  &7Duur: &f{duration} &8| &7Verloopt: &f{expires_at}");
    }

    public String getHistoryEntryHandledBy() {
        return config.getString("history.entry-handled-by", "&8  &7Afgehandeld door: &f{actor} &8| &7Op: &f{removed_at}");
    }

    public String getHistoryEntryHandledReason() {
        return config.getString("history.entry-handled-reason", "&8  &7Afhandel reden: &f{reason}");
    }

    public String getHistoryPaginationPrefix() {
        return config.getString("history.pagination-prefix", "&7Pagina's: ");
    }

    public String getHistoryPaginationHover() {
        return config.getString("history.pagination-hover", "Ga naar pagina {page}");
    }

    public String getHistoryStatusLabel() {
        return config.getString("history.status-label", "&8| &7Status: {status}");
    }

    public String getHistoryStatusActive() {
        return config.getString("history.status-active", "&cActief");
    }

    public String getHistoryStatusExpired() {
        return config.getString("history.status-expired", "&bVerlopen");
    }

    public String getHistoryStatusRemoved() {
        return config.getString("history.status-removed", "&aOpgeheven");
    }

    public String getClearHistoryUsageMessage() {
        return config.getString("clearhistory.usage", "&eGebruik: &7/clearhistory <speler> <punishmentID/all> <reden>");
    }

    public String getClearHistoryIdInvalidMessage() {
        return config.getString("clearhistory.invalid-id", "&cPunishment ID moet een nummer zijn of &fall&c.");
    }

    public String getClearHistoryEntryNotFoundMessage() {
        return config.getString("clearhistory.entry-not-found", "&cGeen punishment gevonden met ID &f{id}&c.");
    }

    public String getClearHistoryWrongPlayerMessage() {
        return config.getString("clearhistory.wrong-player", "&cDit punishment ID hoort niet bij speler &f{player}&c.");
    }

    public String getClearHistoryActiveBlockedMessage() {
        return config.getString("clearhistory.active-blocked", "&cJe kan geen actieve punishment clearen. Gebruik eerst unban/unmute indien nodig.");
    }

    public String getClearHistoryAlreadyHiddenMessage() {
        return config.getString("clearhistory.already-hidden", "&cDeze history entry is al verborgen.");
    }

    public String getClearHistoryNoEntriesMessage() {
        return config.getString("clearhistory.no-entries", "&cGeen history entries gevonden om te clearen voor &f{player}&c.");
    }

    public String getClearHistoryAllSuccessMessage() {
        return config.getString("clearhistory.all-success", "&a{amount} history entr{suffix} verborgen voor &f{player}&a.");
    }

    public String getClearHistorySingleSuccessMessage() {
        return config.getString("clearhistory.single-success", "&aPunishment ID &f{id} &ais verborgen uit history voor &f{player}&a.");
    }

    public String getDefaultReason(String type) {
        return config.getString("punishments." + type + ".default-reason", "Geen reden opgegeven.");
    }

    public List<String> getWarnPlayerMessage() {
        return config.getStringList("punishments.warn.player-message");
    }

    public String getWarnSenderMessage() {
        return config.getString("punishments.warn.sender-message", "&a{player} is gewaarschuwd. Reden: &f{reason}");
    }

    public String getWarnStaffMessage() {
        return config.getString("punishments.warn.staff-message", "&e{player} werd gewaarschuwd door {actor} &8- &7{reason}");
    }

    public String getBanSenderMessage() {
        return config.getString("punishments.ban.sender-message", "&a{player} is geband voor &e{duration}&a. Reden: &f{reason}");
    }

    public String getBanStaffMessage() {
        return config.getString("punishments.ban.staff-message", "&c{player} werd geband door {actor} &8(&7{duration}&8) &8- &7{reason}");
    }

    public List<String> getBanDisconnectMessage() {
        return config.getStringList("punishments.ban.disconnect-message");
    }

    public String getUnbanSenderMessage() {
        return config.getString("punishments.unban.sender-message", "&a{player} is ge-unbanned. Reden: &f{reason}");
    }

    public String getUnbanStaffMessage() {
        return config.getString("punishments.unban.staff-message", "&a{player} werd ge-unbanned door {actor} &8- &7{reason}");
    }

    public List<String> getUnbanExpiredMessage() {
        return config.getStringList("punishments.unban.expired-message");
    }

    public String getMuteSenderMessage() {
        return config.getString("punishments.mute.sender-message", "&a{player} is gemute voor &e{duration}&a. Reden: &f{reason}");
    }

    public String getMuteStaffMessage() {
        return config.getString("punishments.mute.staff-message", "&e{player} werd gemute door {actor} &8(&7{duration}&8) &8- &7{reason}");
    }

    public List<String> getMutePlayerMessage() {
        return config.getStringList("punishments.mute.player-message");
    }

    public List<String> getMuteBlockedChatMessage() {
        return config.getStringList("punishments.mute.blocked-chat-message");
    }

    public String getUnmuteSenderMessage() {
        return config.getString("punishments.unmute.sender-message", "&a{player} is ge-unmute. Reden: &f{reason}");
    }

    public String getUnmuteStaffMessage() {
        return config.getString("punishments.unmute.staff-message", "&a{player} werd ge-unmute door {actor} &8- &7{reason}");
    }

    public List<String> getUnmutePlayerMessage() {
        return config.getStringList("punishments.unmute.player-message");
    }

    public String getKickSenderMessage() {
        return config.getString("punishments.kick.sender-message", "&a{player} is gekickt. Reden: &f{reason}");
    }

    public String getKickStaffMessage() {
        return config.getString("punishments.kick.staff-message", "&6{player} werd gekickt door {actor} &8- &7{reason}");
    }

    public List<String> getKickDisconnectMessage() {
        return config.getStringList("punishments.kick.disconnect-message");
    }

    public int getBanlistEntriesPerPage() {
        return config.getInt("banlist.entries-per-page", 10);
    }

    public String getBanlistHeader() {
        return config.getString("banlist.header", "&8&m----------------------------------------");
    }

    public String getBanlistTitle() {
        return config.getString("banlist.title", "&cActieve bans &7(Pagina &f{page}&7/&f{max_page}&7)");
    }

    public String getBanlistEntry() {
        return config.getString("banlist.entry", "&7#{index} &f{player} &8- &c{reason}");
    }

    public String getBanlistSubEntry() {
        return config.getString("banlist.sub-entry", "&8   &7Door: &f{actor} &8| &7Duur: &f{duration} &8| &7Tot: &f{expires_at}");
    }

    public String getBanlistFooter() {
        return config.getString("banlist.footer", "&8&m----------------------------------------");
    }

    public String getBanlistEmptyMessage() {
        return config.getString("banlist.empty", "&aEr zijn momenteel geen actieve bans.");
    }

    public String getBanlistInvalidPageMessage() {
        return config.getString("banlist.invalid-page", "&cOngeldige pagina.");
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