package me.lekkernakkie.lekkeradmin.discord.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogChannelConfig;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class MinecraftWebhookLogger {

    private final LekkerAdmin plugin;

    public MinecraftWebhookLogger(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void send(LogTypeSettings settings, MinecraftLogMessage message) {
        if (settings == null || message == null) {
            return;
        }

        LogChannelConfig channelConfig = settings.getChannelConfig();
        if (channelConfig == null || channelConfig.getWebhookUrl().isBlank()) {
            plugin.getLogger().warning("Minecraft webhook log url is empty.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(channelConfig.getWebhookUrl()).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                String payload = buildPayload(channelConfig, message);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Minecraft webhook returned response code: " + responseCode);
                }

                connection.disconnect();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to send Minecraft webhook log: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private String buildPayload(LogChannelConfig channelConfig, MinecraftLogMessage message) {
        String username = escapeJson(channelConfig.getWebhookUsername());
        String avatarUrl = escapeJson(channelConfig.getWebhookAvatarUrl());
        String content = escapeJson(message.getContent());

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"username\":\"").append(username).append("\"");

        if (!avatarUrl.isBlank()) {
            json.append(",\"avatar_url\":\"").append(avatarUrl).append("\"");
        }

        if (message.hasContent()) {
            json.append(",\"content\":\"").append(content).append("\"");
        }

        if (message.hasEmbed()) {
            json.append(",\"embeds\":[");
            json.append(buildEmbedJson(message));
            json.append("]");
        }

        json.append("}");
        return json.toString();
    }

    private String buildEmbedJson(MinecraftLogMessage message) {
        String title = message.getEmbed().getTitle() == null ? "" : escapeJson(message.getEmbed().getTitle());
        String description = message.getEmbed().getDescription() == null ? "" : escapeJson(message.getEmbed().getDescription());
        int color = message.getEmbed().getColorRaw();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"title\":\"").append(title).append("\"");
        json.append(",\"description\":\"").append(description).append("\"");
        json.append(",\"color\":").append(color);

        if (message.getEmbed().getFooter() != null && message.getEmbed().getFooter().getText() != null) {
            json.append(",\"footer\":{\"text\":\"")
                    .append(escapeJson(message.getEmbed().getFooter().getText()))
                    .append("\"}");
        }

        if (message.getEmbed().getThumbnail() != null && message.getEmbed().getThumbnail().getUrl() != null) {
            json.append(",\"thumbnail\":{\"url\":\"")
                    .append(escapeJson(message.getEmbed().getThumbnail().getUrl()))
                    .append("\"}");
        }

        if (message.getEmbed().getTimestamp() != null) {
            json.append(",\"timestamp\":\"").append(message.getEmbed().getTimestamp().toString()).append("\"");
        }

        if (!message.getEmbed().getFields().isEmpty()) {
            json.append(",\"fields\":[");
            for (int i = 0; i < message.getEmbed().getFields().size(); i++) {
                var field = message.getEmbed().getFields().get(i);
                if (i > 0) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"name\":\"").append(escapeJson(field.getName())).append("\",")
                        .append("\"value\":\"").append(escapeJson(field.getValue())).append("\",")
                        .append("\"inline\":").append(field.isInline())
                        .append("}");
            }
            json.append("]");
        }

        json.append("}");
        return json.toString();
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