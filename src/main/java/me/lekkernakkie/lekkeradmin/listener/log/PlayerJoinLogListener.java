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
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinLogListener implements Listener {

    private final LekkerAdmin plugin;
    private final MinecraftLogDispatcher dispatcher;

    public PlayerJoinLogListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.dispatcher = new MinecraftLogDispatcher(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return;
        }

        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getPlayerJoins();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerJoinLeaveLogContext context = new PlayerJoinLeaveLogContext(
                player.getName(),
                "JOIN",
                LocationLogUtil.formatWorld(player.getLocation()),
                LocationLogUtil.formatCoordinates(player.getLocation()),
                formatHealth(player),
                player.getFoodLevel(),
                player.getGameMode().name()
        );

        dispatcher.dispatchJoin(settings, context);
    }

    private String formatHealth(Player player) {
        return String.format("%.1f / %.1f", player.getHealth(), player.getMaxHealth());
    }
}