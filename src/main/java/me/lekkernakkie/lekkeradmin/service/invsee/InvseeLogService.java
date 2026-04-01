package me.lekkernakkie.lekkeradmin.service.invsee;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.discord.log.InvseeLogDispatcher;
import me.lekkernakkie.lekkeradmin.model.invsee.InvseeSession;
import me.lekkernakkie.lekkeradmin.model.log.InvseeLogContext;
import me.lekkernakkie.lekkeradmin.util.TimeUtil;

import java.util.List;

public class InvseeLogService {

    private final LekkerAdmin plugin;
    private final InvseeLogDispatcher dispatcher;

    public InvseeLogService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.dispatcher = new InvseeLogDispatcher(plugin);
    }

    public void logSession(InvseeSession session) {
        if (session == null || plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return;
        }

        LogTypeSettings settings = session.isEnderChest()
                ? plugin.getConfigManager().getLogsConfig().getEnderchestLogs()
                : plugin.getConfigManager().getLogsConfig().getInvseeLogs();

        if (settings == null || !settings.isEnabled()) {
            return;
        }

        List<String> actions = session.getActions();

        InvseeLogContext context = new InvseeLogContext(
                session.getViewerName(),
                session.getTarget().getName() == null ? "-" : session.getTarget().getName(),
                session.isOnlineTarget(),
                session.isEnderChest(),
                TimeUtil.formatMillis(session.getOpenedAt()),
                TimeUtil.formatMillis(session.getClosedAt() > 0 ? session.getClosedAt() : System.currentTimeMillis()),
                formatDuration(session.getDurationMillis()),
                actions
        );

        dispatcher.dispatch(settings, context);
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        if (minutes <= 0) {
            return seconds + "s";
        }

        return minutes + "m " + seconds + "s";
    }
}