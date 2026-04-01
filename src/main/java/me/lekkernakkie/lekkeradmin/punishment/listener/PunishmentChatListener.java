package me.lekkernakkie.lekkeradmin.punishment.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.PunishmentsConfig;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.punishment.service.PunishmentService;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.Optional;

public class PunishmentChatListener implements Listener {

    private final PunishmentService punishmentService;
    private final PunishmentsConfig config;

    public PunishmentChatListener(LekkerAdmin plugin) {
        this.punishmentService = plugin.getPunishmentService();
        this.config = plugin.getConfigManager().getPunishmentsConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        String minecraftName = event.getPlayer().getName();
        String minecraftUuid = event.getPlayer().getUniqueId().toString();

        Optional<PunishmentEntry> activeMute = punishmentService.getActiveMute(minecraftName, minecraftUuid);
        if (activeMute.isEmpty()) {
            return;
        }

        PunishmentEntry entry = activeMute.get();
        event.setCancelled(true);

        String remaining = PunishmentFormatter.formatRemaining(
                entry.getExpiresAt(),
                config.getDateFormat(),
                config.getTimezone()
        );

        List<String> lines = PunishmentFormatter.apply(
                config.getMuteBlockedChatMessage(),
                entry.getMinecraftName(),
                PunishmentFormatter.valueOrUnknown(entry.getIssuedByName()),
                PunishmentFormatter.valueOrUnknown(entry.getReason()),
                remaining,
                PunishmentFormatter.formatDate(entry.getExpiresAt(), config.getDateFormat(), config.getTimezone()),
                config.getServerName()
        );

        lines.forEach(event.getPlayer()::sendMessage);
    }
}