package me.lekkernakkie.lekkeradmin.listener.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftLogDispatcher;
import me.lekkernakkie.lekkeradmin.model.log.PlayerJoinLeaveLogContext;
import me.lekkernakkie.lekkeradmin.util.log.LocationLogUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLeaveLogListener implements Listener {

    private static final Map<UUID, String> KICK_REASONS = new ConcurrentHashMap<>();

    private final LekkerAdmin plugin;
    private final MinecraftLogDispatcher dispatcher;

    public PlayerLeaveLogListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.dispatcher = new MinecraftLogDispatcher(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        String reason = event.getReason();
        if (reason == null || reason.isBlank()) {
            reason = "KICK";
        } else {
            reason = "KICK: " + stripColor(reason);
        }

        KICK_REASONS.put(event.getPlayer().getUniqueId(), reason);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return;
        }

        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getPlayerLeaves();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        String reason = KICK_REASONS.remove(player.getUniqueId());
        if (reason == null || reason.isBlank()) {
            String quitMessage = event.getQuitMessage();
            if (quitMessage == null || quitMessage.isBlank()) {
                reason = "QUIT";
            } else {
                reason = "QUIT";
            }
        }

        PlayerJoinLeaveLogContext context = new PlayerJoinLeaveLogContext(
                player.getName(),
                reason,
                LocationLogUtil.formatWorld(player.getLocation()),
                LocationLogUtil.formatCoordinates(player.getLocation()),
                formatHealth(player),
                player.getFoodLevel(),
                player.getGameMode().name()
        );

        dispatcher.dispatchLeave(settings, context);
    }

    private String formatHealth(Player player) {
        return String.format("%.1f / %.1f", player.getHealth(), player.getMaxHealth());
    }

    private String stripColor(String input) {
        return input == null ? "" : input.replaceAll("§[0-9a-fk-or]", "");
    }
}