package me.lekkernakkie.lekkeradmin.config.logs;

public class LogChannelConfig {

    private final String channelId;
    private final String webhookUrl;
    private final String webhookUsername;
    private final String webhookAvatarUrl;

    public LogChannelConfig(String channelId, String webhookUrl, String webhookUsername, String webhookAvatarUrl) {
        this.channelId = channelId == null ? "" : channelId;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl;
        this.webhookUsername = webhookUsername == null ? "" : webhookUsername;
        this.webhookAvatarUrl = webhookAvatarUrl == null ? "" : webhookAvatarUrl;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getWebhookUsername() {
        return webhookUsername;
    }

    public String getWebhookAvatarUrl() {
        return webhookAvatarUrl;
    }
}