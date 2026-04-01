package me.lekkernakkie.lekkeradmin.punishment.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.PunishmentsConfig;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.punishment.service.PunishmentService;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.List;
import java.util.Optional;

public class PunishmentLoginListener implements Listener {

    private final PunishmentService punishmentService;
    private final PunishmentsConfig config;
    private final LekkerAdmin plugin;

    public PunishmentLoginListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.punishmentService = plugin.getPunishmentService();
        this.config = plugin.getConfigManager().getPunishmentsConfig();
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        String minecraftName = event.getPlayer().getName();
        String minecraftUuid = event.getPlayer().getUniqueId().toString();

        Optional<PunishmentEntry> activeBan = punishmentService.getActiveBan(minecraftName, minecraftUuid);
        if (activeBan.isEmpty()) {
            return;
        }

        PunishmentEntry entry = activeBan.get();

        String duration = PunishmentFormatter.formatRemaining(
                entry.getExpiresAt(),
                config.getDateFormat(),
                config.getTimezone()
        );

        String expiresAt = PunishmentFormatter.formatDate(
                entry.getExpiresAt(),
                config.getDateFormat(),
                config.getTimezone()
        );

        List<String> lines = PunishmentFormatter.apply(
                plugin.lang().getList("punishments.ban.disconnect-message", List.of(
                        "&cJe bent geband van de server.",
                        "",
                        "&7Reden: &b{reason}",
                        "&7Door: &b{actor}",
                        "&7Duur: &b{duration}",
                        "&7Tot: &b{expires_at}"
                )),
                entry.getMinecraftName(),
                PunishmentFormatter.valueOrUnknown(entry.getIssuedByName()),
                PunishmentFormatter.valueOrUnknown(entry.getReason()),
                duration,
                expiresAt,
                config.getServerName()
        );

        event.disallow(PlayerLoginEvent.Result.KICK_BANNED, String.join("\n", lines));
    }
}