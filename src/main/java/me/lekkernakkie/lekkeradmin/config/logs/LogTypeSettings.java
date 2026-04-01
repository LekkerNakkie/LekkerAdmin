package me.lekkernakkie.lekkeradmin.config.logs;

public class LogTypeSettings {

    private final boolean enabled;
    private final boolean useEmbeds;
    private final String deliveryMode;
    private final boolean logInvsee;
    private final boolean logEnderchest;
    private final LogChannelConfig channelConfig;
    private final LogFilterConfig filterConfig;
    private final LogEmbedConfig embedConfig;
    private final LogAggregationConfig aggregationConfig;

    public LogTypeSettings(
            boolean enabled,
            boolean useEmbeds,
            String deliveryMode,
            boolean logInvsee,
            boolean logEnderchest,
            LogChannelConfig channelConfig,
            LogFilterConfig filterConfig,
            LogEmbedConfig embedConfig,
            LogAggregationConfig aggregationConfig
    ) {
        this.enabled = enabled;
        this.useEmbeds = useEmbeds;
        this.deliveryMode = deliveryMode == null || deliveryMode.isBlank() ? "BOT" : deliveryMode.toUpperCase();
        this.logInvsee = logInvsee;
        this.logEnderchest = logEnderchest;
        this.channelConfig = channelConfig;
        this.filterConfig = filterConfig;
        this.embedConfig = embedConfig;
        this.aggregationConfig = aggregationConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isUseEmbeds() {
        return useEmbeds;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public boolean isLogInvsee() {
        return logInvsee;
    }

    public boolean isLogEnderchest() {
        return logEnderchest;
    }

    public LogChannelConfig getChannelConfig() {
        return channelConfig;
    }

    public LogFilterConfig getFilterConfig() {
        return filterConfig;
    }

    public LogEmbedConfig getEmbedConfig() {
        return embedConfig;
    }

    public LogAggregationConfig getAggregationConfig() {
        return aggregationConfig;
    }
}