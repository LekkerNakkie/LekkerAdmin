package me.lekkernakkie.lekkeradmin.punishment.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.repository.PunishmentRepository;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public class PunishmentNotifyJoinListener implements Listener {

    private final PunishmentRepository repository;

    public PunishmentNotifyJoinListener(LekkerAdmin plugin) {
        this.repository = new PunishmentRepository(plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        List<PunishmentEntry> pending = repository.getPendingNotifications(uuid);

        if (pending.isEmpty()) return;

        for (PunishmentEntry entry : pending) {

            switch (entry.getPunishmentType()) {

                case WARN -> event.getPlayer().sendMessage(
                        "§cJe hebt een waarschuwing gekregen\n" +
                                "§7Door: §f" + entry.getIssuedByName() + "\n" +
                                "§7Reden: §f" + entry.getReason()
                );

                case MUTE -> event.getPlayer().sendMessage(
                        "§cJe bent gemute\n" +
                                "§7Door: §f" + entry.getIssuedByName() + "\n" +
                                "§7Reden: §f" + entry.getReason()
                );
            }

            repository.markNotificationDelivered(entry.getId());
        }
    }
}