package me.lekkernakkie.lekkeradmin.punishment.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.PunishmentsConfig;
import me.lekkernakkie.lekkeradmin.database.repository.PunishmentRepository;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public class PunishmentNotifyJoinListener implements Listener {

    private final PunishmentRepository repository;
    private final LekkerAdmin plugin;
    private final PunishmentsConfig config;

    public PunishmentNotifyJoinListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.repository = new PunishmentRepository(plugin);
        this.config = plugin.getConfigManager().getPunishmentsConfig();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        List<PunishmentEntry> pending = repository.getPendingNotifications(uuid);
        if (pending.isEmpty()) {
            return;
        }

        for (PunishmentEntry entry : pending) {
            switch (entry.getPunishmentType()) {
                case WARN -> {
                    String date = PunishmentFormatter.formatDate(
                            entry.getIssuedAt(),
                            config.getDateFormat(),
                            config.getTimezone()
                    );

                    List<String> lines = PunishmentFormatter.apply(
                            plugin.lang().getList("punishments.warn.player-message", List.of(
                                    "&cJe hebt een waarschuwing gekregen.",
                                    "",
                                    "&7Door: &b{actor}",
                                    "&7Reden: &b{reason}",
                                    "&7Datum: &b{date}"
                            )),
                            entry.getMinecraftName(),
                            PunishmentFormatter.valueOrUnknown(entry.getIssuedByName()),
                            PunishmentFormatter.valueOrUnknown(entry.getReason()),
                            null,
                            null,
                            config.getServerName(),
                            date
                    );

                    lines.forEach(event.getPlayer()::sendMessage);
                }

                case MUTE -> {
                    List<String> lines = PunishmentFormatter.apply(
                            plugin.lang().getList("punishments.mute.player-message", List.of(
                                    "&cJe bent gemute.",
                                    "",
                                    "&7Door: &b{actor}",
                                    "&7Reden: &b{reason}",
                                    "&7Duur: &b{duration}"
                            )),
                            entry.getMinecraftName(),
                            PunishmentFormatter.valueOrUnknown(entry.getIssuedByName()),
                            PunishmentFormatter.valueOrUnknown(entry.getReason()),
                            PunishmentFormatter.formatRemaining(entry.getExpiresAt(), config.getDateFormat(), config.getTimezone()),
                            PunishmentFormatter.formatDate(entry.getExpiresAt(), config.getDateFormat(), config.getTimezone()),
                            config.getServerName()
                    );

                    lines.forEach(event.getPlayer()::sendMessage);
                }
            }

            repository.markNotificationDelivered(entry.getId());
        }
    }
}