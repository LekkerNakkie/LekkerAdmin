package me.lekkernakkie.lekkeradmin.discord.message;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookService {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;

    public DiscordWebhookService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public void sendWebhook(String content) {
        if (!config.isWebhookEnabled() || !config.isWebhookLoggingEnabled()) {
            return;
        }

        String webhookUrl = config.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            plugin.getLogger().warning("Webhook logging is enabled, but webhook url is empty.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                String payload = buildPayload(content);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Discord webhook returned response code: " + responseCode);
                }

                connection.disconnect();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private String buildPayload(String content) {
        String username = escapeJson(config.getWebhookUsername());
        String avatarUrl = escapeJson(config.getWebhookAvatarUrl());
        String escapedContent = escapeJson(content == null ? "-" : content);

        if (avatarUrl.isBlank()) {
            return "{"
                    + "\"username\":\"" + username + "\","
                    + "\"content\":\"" + escapedContent + "\""
                    + "}";
        }

        return "{"
                + "\"username\":\"" + username + "\","
                + "\"avatar_url\":\"" + avatarUrl + "\","
                + "\"content\":\"" + escapedContent + "\""
                + "}";
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}