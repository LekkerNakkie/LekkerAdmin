package me.lekkernakkie.lekkeradmin.punishment.service;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.repository.PunishmentRepository;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class PunishmentExpiryService {

    private final LekkerAdmin plugin;
    private final PunishmentRepository repository;

    private BukkitTask pollingTask;

    public PunishmentExpiryService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.repository = new PunishmentRepository(plugin);
    }

    public void start() {
        stop();

        long intervalTicks = Math.max(20L, plugin.getConfigManager().getPunishmentsConfig().getExpiryCheckSeconds() * 20L);

        pollingTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                long now = System.currentTimeMillis();
                List<PunishmentEntry> timedPunishments = repository.findActiveTimedPunishments();

                if (timedPunishments == null || timedPunishments.isEmpty()) {
                    return;
                }

                for (PunishmentEntry entry : timedPunishments) {
                    if (entry == null || entry.getExpiresAt() == null) {
                        continue;
                    }

                    if (entry.getExpiresAt() <= now) {
                        plugin.getPunishmentService().processExpiredPunishment(entry);
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("PunishmentExpiryService poll failed: " + ex.getMessage());
            }
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
    }
}