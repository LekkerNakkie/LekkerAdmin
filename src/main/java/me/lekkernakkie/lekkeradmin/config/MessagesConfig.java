package me.lekkernakkie.lekkeradmin.config;

import org.bukkit.configuration.file.FileConfiguration;

public class MessagesConfig {

    private final FileConfiguration config;

    public MessagesConfig(FileConfiguration config) {
        this.config = config;
    }

    public String getSlashWhitelistStarted() {
        return config.getString("messages.slash.whitelist-started",
                "&aIk heb je een DM gestuurd met je whitelist formulier.");
    }

    public String getSlashDmFailed() {
        return config.getString("messages.slash.dm-failed",
                "&cIk kan je geen DM sturen. Zet je privéberichten aan en probeer opnieuw.");
    }

    public String getDmApplicationSubmitted() {
        return config.getString("messages.dm.application-submitted",
                "&aJe whitelist aanvraag is doorgestuurd naar staff.");
    }

    public String getDmApplicationCancelled() {
        return config.getString("messages.dm.application-cancelled",
                "&cJe aanvraag is geannuleerd.");
    }

    public String getDmApplicationApproved() {
        return config.getString("messages.dm.application-approved",
                "&aJe aanvraag is goedgekeurd! Je bent toegevoegd aan de whitelist.");
    }

    public String getDmApplicationDenied() {
        return config.getString("messages.dm.application-denied",
                "&cJe aanvraag is afgekeurd.");
    }

    public String getDmApplicationDeniedWithReason() {
        return config.getString("messages.dm.application-denied-with-reason",
                "&cJe aanvraag is afgekeurd. Reden: &f{reason}");
    }

    public String getDmInvalidMinecraftName() {
        return config.getString("messages.dm.invalid-minecraft-name",
                "&cDe Minecraft naam die je hebt opgegeven klopt niet.");
    }

    public String getDmRetryNameSuccess() {
        return config.getString("messages.dm.retry-name-success",
                "&aJe naam is correct verwerkt. Je bent nu toegevoegd aan de whitelist.");
    }

    public String getDmRetryNameFailed() {
        return config.getString("messages.dm.retry-name-failed",
                "&cDeze naam is nog steeds ongeldig. Probeer opnieuw.");
    }

    public String getStaffReviewTitle() {
        return config.getString("messages.staff.review-title",
                "&bNieuwe whitelist aanvraag");
    }

    public String getStaffApproved() {
        return config.getString("messages.staff.approved",
                "&aAanvraag goedgekeurd.");
    }

    public String getStaffDenied() {
        return config.getString("messages.staff.denied",
                "&cAanvraag afgekeurd.");
    }

    public String getStaffPendingNameFix() {
        return config.getString("messages.staff.pending-name-fix",
                "&eAanvraag goedgekeurd, maar Minecraft naam is ongeldig.");
    }

    public String getStaffRetryProcessed() {
        return config.getString("messages.staff.retry-processed",
                "&aNaamcorrectie succesvol verwerkt.");
    }
}