package me.lekkernakkie.lekkeradmin.listener.freeze;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.freeze.FreezeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FreezeJoinQuitListener implements Listener {

    private final FreezeService freezeService;

    public FreezeJoinQuitListener(LekkerAdmin plugin) {
        this.freezeService = plugin.getFreezeService();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        freezeService.restoreOnJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        freezeService.handleQuit(event.getPlayer());
    }
}