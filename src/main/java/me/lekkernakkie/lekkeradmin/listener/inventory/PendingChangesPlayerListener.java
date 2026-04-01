package me.lekkernakkie.lekkeradmin.listener.inventory;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

public class PendingChangesPlayerListener implements Listener {

    private final LekkerAdmin plugin;

    public PendingChangesPlayerListener(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                plugin.getInvseeService().handleTargetJoinWhileOfflineEditing(player);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to flush offline invsee session on join for "
                        + player.getName() + ": " + ex.getMessage());
                ex.printStackTrace();

                player.sendMessage(plugin.lang().formatMessage(
                        "invsee.flush-join-failed",
                        "&cKon offline invsee sessie niet verwerken bij join van &b{player}&7.",
                        Map.of("player", player.getName())
                ));
            }
        });
    }
}