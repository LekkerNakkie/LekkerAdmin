package me.lekkernakkie.lekkeradmin.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class MainConfig {

    private final FileConfiguration config;

    public MainConfig(FileConfiguration config) {
        this.config = config;
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }

    public String getPrefix() {
        return config.getString("prefix", "&7[&bLekkerAdmin&7] ");
    }

    public boolean isInvseeEnabled() {
        return config.getBoolean("invsee.enabled", true);
    }

    public boolean isInvseeAllowOffline() {
        return config.getBoolean("invsee.allow-offline", true);
    }

    public boolean isInvseeAllowModify() {
        return config.getBoolean("invsee.allow-modify", true);
    }

    public String getInvseeTitle() {
        return config.getString("invsee.title", "&bInvsee &7• &f{player}");
    }

    public int getInvseeRows() {
        int rows = config.getInt("invsee.rows", 6);
        return Math.max(1, Math.min(6, rows));
    }

    public boolean isEnderchestEnabled() {
        return config.getBoolean("enderchest.enabled", true);
    }

    public boolean isEnderchestAllowOffline() {
        return config.getBoolean("enderchest.allow-offline", true);
    }

    public boolean isEnderchestAllowModify() {
        return config.getBoolean("enderchest.allow-modify", true);
    }

    public String getEnderchestTitle() {
        return config.getString("enderchest.title", "&5Enderchest &7• &f{player}");
    }

    public boolean isLeaveTrackingEnabled() {
        return config.getBoolean("leave-tracking.enabled", true);
    }

    public boolean isLeaveTrackingClassifyKicks() {
        return config.getBoolean("leave-tracking.classify-kicks", true);
    }

    public String getLeaveTrackingFallbackQuitReason() {
        return config.getString("leave-tracking.fallback-quit-reason", "QUIT");
    }

    public String getInvseeEquipmentLabelHelmet() {
        return config.getString("invsee.layout.labels.helmet", "&bHelm");
    }

    public String getInvseeEquipmentLabelChestplate() {
        return config.getString("invsee.layout.labels.chestplate", "&bChestplate");
    }

    public String getInvseeEquipmentLabelLeggings() {
        return config.getString("invsee.layout.labels.leggings", "&bLeggings");
    }

    public String getInvseeEquipmentLabelBoots() {
        return config.getString("invsee.layout.labels.boots", "&bBoots");
    }

    public String getInvseeEquipmentLabelOffhand() {
        return config.getString("invsee.layout.labels.offhand", "&bOffhand");
    }

    public int getInvseeHelmetSlot() {
        return config.getInt("invsee.layout.slots.helmet", 46);
    }

    public int getInvseeChestplateSlot() {
        return config.getInt("invsee.layout.slots.chestplate", 47);
    }

    public int getInvseeLeggingsSlot() {
        return config.getInt("invsee.layout.slots.leggings", 48);
    }

    public int getInvseeBootsSlot() {
        return config.getInt("invsee.layout.slots.boots", 49);
    }

    public int getInvseeOffhandSlot() {
        return config.getInt("invsee.layout.slots.offhand", 53);
    }

    public int getInvseeHelmetLabelSlot() {
        return config.getInt("invsee.layout.label-slots.helmet", 37);
    }

    public int getInvseeChestplateLabelSlot() {
        return config.getInt("invsee.layout.label-slots.chestplate", 38);
    }

    public int getInvseeLeggingsLabelSlot() {
        return config.getInt("invsee.layout.label-slots.leggings", 39);
    }

    public int getInvseeBootsLabelSlot() {
        return config.getInt("invsee.layout.label-slots.boots", 40);
    }

    public int getInvseeOffhandLabelSlot() {
        return config.getInt("invsee.layout.label-slots.offhand", 44);
    }

    public boolean isRestartEnabled() {
        return config.getBoolean("restart.enabled", true);
    }

    public String getRestartTimezone() {
        return config.getString("restart.timezone", "Europe/Brussels");
    }

    public boolean isAutoRestartEnabled() {
        return config.getBoolean("restart.auto.enabled", true);
    }

    public List<String> getAutoRestartTimes() {
        return config.getStringList("restart.auto.times");
    }

    public boolean isAutoRestartSkipIfPlannedRunning() {
        return config.getBoolean("restart.auto.skip-if-planned-running", true);
    }

    public String getAutoRestartDefaultReason() {
        return config.getString("restart.auto.default-reason", "Automatische restart");
    }

    public List<Integer> getRestartAnnounceSeconds() {
        return config.getIntegerList("restart.announce-seconds");
    }

    public String getRestartPrefix() {
        return config.getString("restart.messages.prefix", "&7[&cRestart&7] &7");
    }

    public String getRestartChatMessage() {
        return config.getString("restart.messages.chat", "{prefix}&7Server restart over &e{time}&7. Reden: &e{reason}");
    }

    public String getRestartChatNowMessage() {
        return config.getString("restart.messages.chat-now", "{prefix}&cServer restart nu! Reden: &f{reason}");
    }

    public boolean isRestartTitleEnabled() {
        return config.getBoolean("restart.title.enabled", true);
    }

    public int getRestartTitleFadeIn() {
        return config.getInt("restart.title.fade-in", 10);
    }

    public int getRestartTitleStay() {
        return config.getInt("restart.title.stay", 40);
    }

    public int getRestartTitleFadeOut() {
        return config.getInt("restart.title.fade-out", 10);
    }

    public String getRestartTitleMain() {
        return config.getString("restart.title.main", "&7Restart over &e{time}");
    }

    public String getRestartTitleSub() {
        return config.getString("restart.title.sub", "&7Reden: &f{reason}");
    }

    public boolean isRestartDisconnectEnabled() {
        return config.getBoolean("restart.disconnect.enabled", true);
    }

    public String getRestartKickMessage() {
        return config.getString("restart.disconnect.kick-message", "&cServer restart!\n\n&7Reden: &f{reason}\n&7Je kan direct terug joinen.");
    }

    public String getRestartActionType() {
        return config.getString("restart.action.type", "stop");
    }

    public String getRestartActionCommand() {
        return config.getString("restart.action.command", "stop");
    }

    public boolean isPlanRestartAllowOverride() {
        return config.getBoolean("planrestart.allow-override", true);
    }

    public String getPlanRestartDefaultReason() {
        return config.getString("planrestart.default-reason", "Geen reden opgegeven.");
    }

    public String getPlanRestartNoPermissionMessage() {
        return config.getString("planrestart.messages.no-permission", "&cDaar edde gij het lef ni vur..");
    }

    public String getPlanRestartUsageMessage() {
        return config.getString("planrestart.messages.usage", "&eGebruik: &7/planrestart <time> <reden...>");
    }

    public String getPlanRestartBadTimeMessage() {
        return config.getString("planrestart.messages.bad-time", "&cOngeldige tijd. Gebruik bv: &710s, 5m, 2h, 1h30m");
    }

    public String getPlanRestartAlreadyPlannedMessage() {
        return config.getString("planrestart.messages.already-planned", "&cEr is al een restart gepland en overschrijven is uitgeschakeld.");
    }

    public String getPlanRestartPlannedMessage() {
        return config.getString("planrestart.messages.planned", "&7Restart gepland over &e{time}&7. Reden: &e{reason}&7. Uitvoering om &e{target-time}&7.");
    }

    public String getCancelRestartNoPermissionMessage() {
        return config.getString("cancelrestart.messages.no-permission", "&cDaar edde gij het lef ni vur..");
    }

    public String getCancelRestartNoneRunningMessage() {
        return config.getString("cancelrestart.messages.none-running", "&cEr is momenteel geen restart gepland.");
    }

    public String getCancelRestartCancelledMessage() {
        return config.getString("cancelrestart.messages.cancelled", "&eDe geplande restart is geannuleerd.");
    }
    public boolean isMaintenanceEnabled() {
        return config.getBoolean("maintenance.enabled", false);
    }

    public String getMaintenanceNoPermissionMessage() {
        return config.getString("maintenance.messages.no-permission", "&cDaar edde gij het lef ni vur..");
    }

    public String getMaintenanceToggledOnMessage() {
        return config.getString("maintenance.messages.toggled-on", "&aMaintenance mode is ingeschakeld.");
    }

    public String getMaintenanceToggledOffMessage() {
        return config.getString("maintenance.messages.toggled-off", "&cMaintenance mode is uitgeschakeld.");
    }

    public String getMaintenanceOnlineKickMessage() {
        return config.getString("maintenance.kick.online-message", "&cServer staat in maintenance mode.\n\n&7Probeer later opnieuw.");
    }

    public String getMaintenanceJoinKickMessage() {
        return config.getString("maintenance.kick.join-message", "&cServer staat in maintenance mode.\n\n&7Probeer later opnieuw.");
    }
}