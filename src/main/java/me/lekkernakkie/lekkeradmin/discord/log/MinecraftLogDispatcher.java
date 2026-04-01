package me.lekkernakkie.lekkeradmin.discord.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.discord.embed.MinecraftLogEmbedFactory;
import me.lekkernakkie.lekkeradmin.model.log.ExplosionLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerDeathLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerDropLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerJoinLeaveLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerPickupLogContext;

public class MinecraftLogDispatcher {

    private final LekkerAdmin plugin;
    private final MinecraftBotLogger botLogger;
    private final MinecraftWebhookLogger webhookLogger;
    private final MinecraftLogEmbedFactory embedFactory;

    public MinecraftLogDispatcher(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.botLogger = new MinecraftBotLogger(plugin);
        this.webhookLogger = new MinecraftWebhookLogger(plugin);
        this.embedFactory = new MinecraftLogEmbedFactory(plugin);
    }

    public void dispatchDrop(LogTypeSettings settings, PlayerDropLogContext context) {
        if (!canSend(settings)) {
            return;
        }

        MinecraftLogMessage message = settings.isUseEmbeds()
                ? embedFactory.createDropMessage(settings, context)
                : new MinecraftLogMessage(context.toPlainText(), null, null);

        send(settings, message);
    }

    public void dispatchPickup(LogTypeSettings settings, PlayerPickupLogContext context) {
        if (!canSend(settings)) {
            return;
        }

        MinecraftLogMessage message = settings.isUseEmbeds()
                ? embedFactory.createPickupMessage(settings, context)
                : new MinecraftLogMessage(context.toPlainText(), null, null);

        send(settings, message);
    }

    public void dispatchDeath(LogTypeSettings settings, PlayerDeathLogContext context) {
        if (!canSend(settings)) {
            return;
        }

        MinecraftLogMessage message = settings.isUseEmbeds()
                ? embedFactory.createDeathMessage(settings, context)
                : new MinecraftLogMessage(context.toPlainText(), null, context.getOverflowDroppedItems());

        send(settings, message);
    }

    public void dispatchJoin(LogTypeSettings settings, PlayerJoinLeaveLogContext context) {
        if (!canSend(settings)) {
            return;
        }

        MinecraftLogMessage message = settings.isUseEmbeds()
                ? embedFactory.createJoinMessage(settings, context)
                : new MinecraftLogMessage(context.toPlainText("JOIN"), null, null);

        send(settings, message);
    }

    public void dispatchLeave(LogTypeSettings settings, PlayerJoinLeaveLogContext context) {
        if (!canSend(settings)) {
            return;
        }

        MinecraftLogMessage message = settings.isUseEmbeds()
                ? embedFactory.createLeaveMessage(settings, context)
                : new MinecraftLogMessage(context.toPlainText("LEAVE"), null, null);

        send(settings, message);
    }

    public void dispatchExplosion(LogTypeSettings settings, ExplosionLogContext context) {
        if (!canSend(settings)) {
            return;
        }

        MinecraftLogMessage message = settings.isUseEmbeds()
                ? embedFactory.createExplosionMessage(settings, context)
                : new MinecraftLogMessage(context.toPlainText(), null, null);

        send(settings, message);
    }

    private void send(LogTypeSettings settings, MinecraftLogMessage message) {
        MinecraftLogDeliveryMode mode = MinecraftLogDeliveryMode.fromString(settings.getDeliveryMode());

        switch (mode) {
            case BOT -> botLogger.send(settings, message);
            case WEBHOOK -> webhookLogger.send(settings, message);
            case BOTH -> {
                botLogger.send(settings, message);
                webhookLogger.send(settings, message);
            }
            case NONE -> plugin.debug("MinecraftLogDispatcher skipped: delivery mode NONE.");
        }
    }

    private boolean canSend(LogTypeSettings settings) {
        return plugin.getConfigManager() != null
                && plugin.getConfigManager().getLogsConfig() != null
                && plugin.getConfigManager().getLogsConfig().isEnabled()
                && settings != null
                && settings.isEnabled();
    }
}