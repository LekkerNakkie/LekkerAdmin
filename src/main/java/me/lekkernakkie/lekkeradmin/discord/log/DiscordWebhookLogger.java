package me.lekkernakkie.lekkeradmin.discord.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.discord.message.DiscordWebhookService;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class DiscordWebhookLogger {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;
    private final DiscordWebhookService webhookService;
    private final DiscordAuditFormatter formatter;

    public DiscordWebhookLogger(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.webhookService = new DiscordWebhookService(plugin);
        this.formatter = new DiscordAuditFormatter(plugin);
    }

    public void logApproval(String reviewer, WhitelistApplication application) {
        if (!config.shouldLogApprovals()) {
            return;
        }

        String message = formatter.formatApproval(reviewer, application);
        dispatch(message);
    }

    public void logDenial(String reviewer, WhitelistApplication application, String reason) {
        if (!config.shouldLogDenials()) {
            return;
        }

        String message = formatter.formatDenial(reviewer, application, reason);
        dispatch(message);
    }

    public void logInvalidName(WhitelistApplication application) {
        if (!config.shouldLogNameFailures()) {
            return;
        }

        String message = formatter.formatInvalidName(application);
        dispatch(message);
    }

    public void logRetrySuccess(WhitelistApplication application, String oldName, String newName) {
        if (!config.shouldLogNameRetries() && !config.shouldLogAutoCompletions()) {
            return;
        }

        String message = formatter.formatRetrySuccess(application, oldName, newName);
        dispatch(message);
    }

    private void dispatch(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        if (config.isWebhookLoggingEnabled() && config.isWebhookEnabled()) {
            webhookService.sendWebhook(message);
        }

        sendToLogChannel(message);
    }

    private void sendToLogChannel(String message) {
        String logChannelId = config.getLogChannelId();
        if (logChannelId == null || logChannelId.isBlank() || logChannelId.equals("000000000000000000")) {
            return;
        }

        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) {
            return;
        }

        TextChannel channel = plugin.getDiscordManager().getJda().getTextChannelById(logChannelId);
        if (channel == null) {
            plugin.getLogger().warning("Discord log channel not found: " + logChannelId);
            return;
        }

        channel.sendMessage(message).queue(
                success -> {},
                error -> plugin.getLogger().warning("Failed to send audit log to Discord log channel: " + error.getMessage())
        );
    }
}