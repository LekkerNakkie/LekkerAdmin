package me.lekkernakkie.lekkeradmin.discord.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.discord.embed.InvseeLogEmbedFactory;
import me.lekkernakkie.lekkeradmin.model.log.InvseeLogContext;

public class InvseeLogDispatcher {

    private final LekkerAdmin plugin;
    private final MinecraftBotLogger botLogger;
    private final MinecraftWebhookLogger webhookLogger;
    private final InvseeLogEmbedFactory embedFactory;

    public InvseeLogDispatcher(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.botLogger = new MinecraftBotLogger(plugin);
        this.webhookLogger = new MinecraftWebhookLogger(plugin);
        this.embedFactory = new InvseeLogEmbedFactory(plugin);
    }

    public void dispatch(LogTypeSettings settings, InvseeLogContext context) {
        if (plugin.getConfigManager() == null
                || plugin.getConfigManager().getLogsConfig() == null
                || !plugin.getConfigManager().getLogsConfig().isEnabled()
                || settings == null
                || !settings.isEnabled()) {
            return;
        }

        MinecraftLogMessage message = settings.isUseEmbeds()
                ? embedFactory.create(settings.getEmbedConfig(), context)
                : new MinecraftLogMessage(context.toPlainText(), null, null);

        MinecraftLogDeliveryMode mode = MinecraftLogDeliveryMode.fromString(settings.getDeliveryMode());

        switch (mode) {
            case BOT -> botLogger.send(settings, message);
            case WEBHOOK -> webhookLogger.send(settings, message);
            case BOTH -> {
                botLogger.send(settings, message);
                webhookLogger.send(settings, message);
            }
            case NONE -> plugin.debug("InvseeLogDispatcher skipped: delivery mode NONE.");
        }
    }
}