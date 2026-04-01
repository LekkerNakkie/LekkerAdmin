package me.lekkernakkie.lekkeradmin.discord.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogChannelConfig;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import org.bukkit.Bukkit;

import java.io.InputStream;
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
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) URI.create(channelConfig.getWebhookUrl()).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                String payload = buildPayload(channelConfig, message);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    String responseBody = readResponseBody(connection);
                    plugin.getLogger().warning("Minecraft webhook returned response code: " + responseCode);
                    if (!responseBody.isBlank()) {
                        plugin.getLogger().warning("Minecraft webhook response body: " + responseBody);
                    }
                    plugin.debug("Minecraft webhook payload: " + payload);
                }

            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to send Minecraft webhook log: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String buildPayload(LogChannelConfig channelConfig, MinecraftLogMessage message) {
        String username = channelConfig.getWebhookUsername() == null
                ? ""
                : escapeJson(channelConfig.getWebhookUsername().trim());

        String avatarUrl = channelConfig.getWebhookAvatarUrl() == null
                ? ""
                : escapeJson(channelConfig.getWebhookAvatarUrl().trim());

        String content = escapeJson(message.getContent());

        StringBuilder json = new StringBuilder();
        json.append("{");

        boolean hasPrevious = false;

        if (!username.isBlank()) {
            json.append("\"username\":\"").append(username).append("\"");
            hasPrevious = true;
        }

        if (!avatarUrl.isBlank()) {
            if (hasPrevious) {
                json.append(",");
            }
            json.append("\"avatar_url\":\"").append(avatarUrl).append("\"");
            hasPrevious = true;
        }

        if (message.hasContent()) {
            if (hasPrevious) {
                json.append(",");
            }
            json.append("\"content\":\"").append(content).append("\"");
            hasPrevious = true;
        }

        if (message.hasEmbed()) {
            if (hasPrevious) {
                json.append(",");
            }
            json.append("\"embeds\":[");
            json.append(buildEmbedJson(message));
            json.append("]");
        }

        json.append("}");
        return json.toString();
    }

    private String buildEmbedJson(MinecraftLogMessage message) {
        String title = message.getEmbed().getTitle() == null ? "" : escapeJson(message.getEmbed().getTitle());
        String description = message.getEmbed().getDescription() == null ? "" : escapeJson(message.getEmbed().getDescription());

        int rawColor = message.getEmbed().getColorRaw();
        int safeColor = rawColor & 0xFFFFFF;

        StringBuilder json = new StringBuilder();
        json.append("{");

        boolean hasPrevious = false;

        if (!title.isBlank()) {
            json.append("\"title\":\"").append(title).append("\"");
            hasPrevious = true;
        }

        if (!description.isBlank()) {
            if (hasPrevious) {
                json.append(",");
            }
            json.append("\"description\":\"").append(description).append("\"");
            hasPrevious = true;
        }

        if (hasPrevious) {
            json.append(",");
        }
        json.append("\"color\":").append(safeColor);
        hasPrevious = true;

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
            json.append(",\"timestamp\":\"")
                    .append(escapeJson(message.getEmbed().getTimestamp().toString()))
                    .append("\"");
        }

        if (!message.getEmbed().getFields().isEmpty()) {
            json.append(",\"fields\":[");
            int writtenFields = 0;

            for (int i = 0; i < message.getEmbed().getFields().size(); i++) {
                var field = message.getEmbed().getFields().get(i);
                if (field == null || field.getName() == null || field.getValue() == null) {
                    continue;
                }

                if (writtenFields >= 25) {
                    break;
                }

                if (writtenFields > 0) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"name\":\"").append(escapeJson(field.getName())).append("\",")
                        .append("\"value\":\"").append(escapeJson(field.getValue())).append("\",")
                        .append("\"inline\":").append(field.isInline())
                        .append("}");

                writtenFields++;
            }

            json.append("]");
        }

        json.append("}");
        return json.toString();
    }

    private String readResponseBody(HttpURLConnection connection) {
        try {
            InputStream stream = connection.getResponseCode() >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();

            if (stream == null) {
                return "";
            }

            byte[] bytes = stream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
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