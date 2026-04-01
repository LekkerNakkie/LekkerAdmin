package me.lekkernakkie.lekkeradmin.discord.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;

public class DiscordAuditFormatter {

    private final DCBotConfig config;

    public DiscordAuditFormatter(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public String formatApproval(String reviewer, WhitelistApplication application) {
        return apply(
                config.getApprovalLogFormat(),
                reviewer,
                application,
                null,
                null,
                null
        );
    }

    public String formatDenial(String reviewer, WhitelistApplication application, String reason) {
        return apply(
                config.getDenialLogFormat(),
                reviewer,
                application,
                reason,
                null,
                null
        );
    }

    public String formatInvalidName(WhitelistApplication application) {
        return apply(
                config.getInvalidNameLogFormat(),
                null,
                application,
                null,
                null,
                null
        );
    }

    public String formatRetrySuccess(WhitelistApplication application, String oldName, String newName) {
        return apply(
                config.getRetrySuccessLogFormat(),
                null,
                application,
                null,
                oldName,
                newName
        );
    }

    private String apply(String format,
                         String reviewer,
                         WhitelistApplication application,
                         String reason,
                         String oldName,
                         String newName) {

        String result = format == null ? "-" : format;

        result = result.replace("{reviewer}", safe(reviewer));
        result = result.replace("{application_id}", application == null ? "-" : safe(application.getApplicationId()));
        result = result.replace("{discord_user}", application == null ? "-" : safe(application.getDiscordTag()));
        result = result.replace("{minecraft_name}", application == null ? "-" : safe(application.getMinecraftName()));
        result = result.replace("{reason}", safe(reason));
        result = result.replace("{old_name}", safe(oldName));
        result = result.replace("{new_name}", safe(newName));

        return result;
    }

    private String safe(String input) {
        return input == null || input.isBlank() ? "-" : input;
    }
}