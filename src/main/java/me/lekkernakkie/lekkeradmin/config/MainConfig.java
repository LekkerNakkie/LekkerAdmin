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
        return config.getString("enderchest.title", "&bEnderchest &7• &f{player}");
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

    public boolean isRestartDisconnectEnabled() {
        return config.getBoolean("restart.disconnect.enabled", true);
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

    public boolean isMaintenanceEnabled() {
        return config.getBoolean("maintenance.enabled", false);
    }
    public boolean isWhoisEnabled() {
        return config.getBoolean("whois.enabled", true);
    }

    public boolean isWhoisUseEssentials() {
        return config.getBoolean("whois.hooks.essentials", true);
    }

    public boolean isWhoisUsePlaceholderApi() {
        return config.getBoolean("whois.hooks.placeholderapi", true);
    }

    public String getWhoisBalancePlaceholder() {
        return config.getString("whois.placeholders.balance", "%vault_eco_balance_formatted%");
    }

    public String getWhoisLevelPlaceholder() {
        return config.getString("whois.placeholders.level", "%lekkerleveling_level%");
    }

    public int getWhoisDefaultPage() {
        return Math.max(1, config.getInt("whois.pages.default-page", 1));
    }

    public int getWhoisMaxPages() {
        return Math.max(1, config.getInt("whois.pages.max-pages", 2));
    }

    public boolean isWhoisClickableNavigation() {
        return config.getBoolean("whois.pages.clickable-navigation", true);
    }

    public boolean isWhoisFieldEnabled(String field) {
        return config.getBoolean("whois.fields." + field, true);
    }

    public List<String> getWhoisPageFields(int page) {
        return config.getStringList("whois.layout.page-" + page);
    }
}