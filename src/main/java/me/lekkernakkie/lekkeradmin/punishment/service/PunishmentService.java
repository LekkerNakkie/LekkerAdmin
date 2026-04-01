package me.lekkernakkie.lekkeradmin.punishment.service;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.PunishmentsConfig;
import me.lekkernakkie.lekkeradmin.database.repository.AuditLogRepository;
import me.lekkernakkie.lekkeradmin.database.repository.PunishmentRepository;
import me.lekkernakkie.lekkeradmin.model.audit.AuditLogEntry;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentSource;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentStatus;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentType;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import me.lekkernakkie.lekkeradmin.service.link.LinkService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentService {

    private final LekkerAdmin plugin;
    private final PunishmentsConfig config;
    private final PunishmentRepository repository;
    private final AuditLogRepository auditLogRepository;
    private final LinkService linkService;
    private final DiscordPunishmentDMService discordPunishmentDMService;

    private final Map<String, PunishmentEntry> activeBanCacheByName = new ConcurrentHashMap<>();
    private final Map<String, PunishmentEntry> activeBanCacheByUuid = new ConcurrentHashMap<>();
    private final Map<String, PunishmentEntry> activeMuteCacheByName = new ConcurrentHashMap<>();
    private final Map<String, PunishmentEntry> activeMuteCacheByUuid = new ConcurrentHashMap<>();
    private final Map<Long, BukkitTask> scheduledExpirations = new ConcurrentHashMap<>();

    public PunishmentService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getPunishmentsConfig();
        this.repository = new PunishmentRepository(plugin);
        this.auditLogRepository = new AuditLogRepository(plugin);
        this.linkService = new LinkService(plugin);
        this.discordPunishmentDMService = new DiscordPunishmentDMService(plugin);
    }

    public void loadCacheAndReschedule() {
        plugin.getDatabaseManager().runAsync(() -> {
            List<PunishmentEntry> bans = repository.findAllActiveBans();
            List<PunishmentEntry> mutes = repository.findAllActiveMutes();
            List<PunishmentEntry> timed = repository.findActiveTimedPunishments();

            activeBanCacheByName.clear();
            activeBanCacheByUuid.clear();
            activeMuteCacheByName.clear();
            activeMuteCacheByUuid.clear();

            for (PunishmentEntry entry : bans) {
                cacheBan(entry);
            }
            for (PunishmentEntry entry : mutes) {
                cacheMute(entry);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                cancelAllScheduledExpirations();
                for (PunishmentEntry entry : timed) {
                    scheduleExpiration(entry);
                }
            });
        });
    }

    public void cancelAllScheduledExpirations() {
        for (BukkitTask task : scheduledExpirations.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        scheduledExpirations.clear();
    }

    public CompletableFuture<Result> banAsync(CommandSender sender, String targetName, Long durationMs, String reason) {
        return plugin.getDatabaseManager().supplyAsync(() -> {
            ResolvedTarget target = resolveTarget(targetName);

            if (isSelfPunish(sender, target)) {
                return Result.fail(config.getCannotPunishSelfMessage());
            }

            Optional<PunishmentEntry> activeBan = repository.findActivePunishment(
                    target.minecraftName(),
                    target.minecraftUuid(),
                    PunishmentType.BAN
            );

            if (activeBan.isPresent()) {
                return Result.fail(config.getAlreadyBannedMessage());
            }

            String finalReason = normalizeReason("ban", reason);
            PunishmentEntry entry = createBaseEntry(sender, target, PunishmentType.BAN, durationMs, finalReason);
            repository.createPunishment(entry);
            cacheBan(entry);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (entry.getExpiresAt() != null) {
                    scheduleExpiration(entry);
                }

                kickIfOnline(target.minecraftName(), buildBanDisconnectMessage(entry));
                broadcastStaff(config.getBanStaffMessage(), entry);

                sender.sendMessage(PunishmentFormatter.apply(
                        config.getBanSenderMessage(),
                        entry.getMinecraftName(),
                        entry.getIssuedByName(),
                        entry.getReason(),
                        PunishmentFormatter.formatDuration(entry.getDurationMs()),
                        PunishmentFormatter.formatDate(entry.getExpiresAt(), config.getDateFormat(), config.getTimezone()),
                        null
                ));
            });

            logAudit("BAN", sender, entry);
            discordPunishmentDMService.sendPunishmentDM(entry);
            plugin.getDiscordPunishmentLogger().logBan(entry);

            return Result.success(entry);
        });
    }

    public CompletableFuture<Result> unbanAsync(CommandSender sender, String targetName, String reason) {
        return plugin.getDatabaseManager().supplyAsync(() -> {
            ResolvedTarget target = resolveTarget(targetName);

            Optional<PunishmentEntry> activeBan = repository.findActivePunishment(
                    target.minecraftName(),
                    target.minecraftUuid(),
                    PunishmentType.BAN
            );

            if (activeBan.isEmpty()) {
                return Result.fail(config.getNotBannedMessage());
            }

            String finalReason = normalizeReason("unban", reason);
            PunishmentEntry entry = activeBan.get();

            repository.markRemoved(
                    entry.getId(),
                    System.currentTimeMillis(),
                    getActorName(sender),
                    getActorUuid(sender),
                    null,
                    null,
                    finalReason
            );

            removeBan(entry);

            Bukkit.getScheduler().runTask(plugin, () -> {
                cancelScheduledExpiration(entry.getId());

                sender.sendMessage(PunishmentFormatter.apply(
                        config.getUnbanSenderMessage(),
                        entry.getMinecraftName(),
                        getActorName(sender),
                        finalReason,
                        null,
                        null,
                        null
                ));

                broadcastStaff(config.getUnbanStaffMessage(), entry, getActorName(sender), finalReason);
            });

            logAudit("UNBAN", sender, entry);
            discordPunishmentDMService.sendUnbanDM(entry.getMinecraftName(), getActorName(sender), finalReason, false);
            plugin.getDiscordPunishmentLogger().logUnban(entry, getActorName(sender), finalReason);

            return Result.success(entry);
        });
    }

    public CompletableFuture<Result> muteAsync(CommandSender sender, String targetName, Long durationMs, String reason) {
        return plugin.getDatabaseManager().supplyAsync(() -> {
            ResolvedTarget target = resolveTarget(targetName);

            if (isSelfPunish(sender, target)) {
                return Result.fail(config.getCannotPunishSelfMessage());
            }

            Optional<PunishmentEntry> activeMute = repository.findActivePunishment(
                    target.minecraftName(),
                    target.minecraftUuid(),
                    PunishmentType.MUTE
            );

            if (activeMute.isPresent()) {
                return Result.fail(config.getAlreadyMutedMessage());
            }

            String finalReason = normalizeReason("mute", reason);
            PunishmentEntry entry = createBaseEntry(sender, target, PunishmentType.MUTE, durationMs, finalReason);
            repository.createPunishment(entry);
            cacheMute(entry);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (entry.getExpiresAt() != null) {
                    scheduleExpiration(entry);
                }

                notifyPlayerIfOnline(target.minecraftName(), config.getMutePlayerMessage(), entry);

                sender.sendMessage(PunishmentFormatter.apply(
                        config.getMuteSenderMessage(),
                        entry.getMinecraftName(),
                        entry.getIssuedByName(),
                        entry.getReason(),
                        PunishmentFormatter.formatDuration(entry.getDurationMs()),
                        PunishmentFormatter.formatDate(entry.getExpiresAt(), config.getDateFormat(), config.getTimezone()),
                        null
                ));

                broadcastStaff(config.getMuteStaffMessage(), entry);
            });

            logAudit("MUTE", sender, entry);
            discordPunishmentDMService.sendPunishmentDM(entry);
            plugin.getDiscordPunishmentLogger().logMute(entry);

            return Result.success(entry);
        });
    }

    public CompletableFuture<Result> unmuteAsync(CommandSender sender, String targetName, String reason) {
        return plugin.getDatabaseManager().supplyAsync(() -> {
            ResolvedTarget target = resolveTarget(targetName);

            Optional<PunishmentEntry> activeMute = repository.findActivePunishment(
                    target.minecraftName(),
                    target.minecraftUuid(),
                    PunishmentType.MUTE
            );

            if (activeMute.isEmpty()) {
                return Result.fail(config.getNotMutedMessage());
            }

            String finalReason = normalizeReason("unmute", reason);
            PunishmentEntry entry = activeMute.get();

            repository.markRemoved(
                    entry.getId(),
                    System.currentTimeMillis(),
                    getActorName(sender),
                    getActorUuid(sender),
                    null,
                    null,
                    finalReason
            );

            removeMute(entry);

            Bukkit.getScheduler().runTask(plugin, () -> {
                cancelScheduledExpiration(entry.getId());

                sender.sendMessage(PunishmentFormatter.apply(
                        config.getUnmuteSenderMessage(),
                        entry.getMinecraftName(),
                        getActorName(sender),
                        finalReason,
                        null,
                        null,
                        null
                ));

                notifySimplePlayerIfOnline(target.minecraftName(), config.getUnmutePlayerMessage(), getActorName(sender), finalReason);
                broadcastStaff(config.getUnmuteStaffMessage(), entry, getActorName(sender), finalReason);
            });

            logAudit("UNMUTE", sender, entry);
            discordPunishmentDMService.sendUnmuteDM(entry.getMinecraftName(), getActorName(sender), finalReason);
            plugin.getDiscordPunishmentLogger().logUnmute(entry, getActorName(sender), finalReason);

            return Result.success(entry);
        });
    }

    public CompletableFuture<Result> kickAsync(CommandSender sender, String targetName, String reason) {
        return plugin.getDatabaseManager().supplyAsync(() -> {
            ResolvedTarget target = resolveTarget(targetName);

            if (isSelfPunish(sender, target)) {
                return Result.fail(config.getCannotPunishSelfMessage());
            }

            String finalReason = normalizeReason("kick", reason);
            PunishmentEntry entry = createBaseEntry(sender, target, PunishmentType.KICK, null, finalReason);
            entry.setStatus(PunishmentStatus.REMOVED);
            entry.setRemovedAt(entry.getIssuedAt());
            entry.setRemoveReason(finalReason);
            repository.createPunishment(entry);

            Bukkit.getScheduler().runTask(plugin, () -> {
                kickIfOnline(target.minecraftName(), buildKickDisconnectMessage(entry));

                sender.sendMessage(PunishmentFormatter.apply(
                        config.getKickSenderMessage(),
                        entry.getMinecraftName(),
                        entry.getIssuedByName(),
                        entry.getReason(),
                        null,
                        null,
                        null
                ));

                broadcastStaff(config.getKickStaffMessage(), entry);
            });

            logAudit("KICK", sender, entry);
            discordPunishmentDMService.sendPunishmentDM(entry);
            plugin.getDiscordPunishmentLogger().logKick(entry);

            return Result.success(entry);
        });
    }

    public CompletableFuture<List<PunishmentEntry>> getActiveBansPageAsync(int page) {
        return plugin.getDatabaseManager().supplyAsync(() -> {
            int perPage = Math.max(1, config.getBanlistEntriesPerPage());
            int offset = Math.max(0, (page - 1) * perPage);
            return repository.findActiveBansPaged(perPage, offset);
        });
    }

    public CompletableFuture<Integer> getBanPageCountAsync() {
        return plugin.getDatabaseManager().supplyAsync(() -> {
            int count = repository.countActiveBans();
            int perPage = Math.max(1, config.getBanlistEntriesPerPage());
            return Math.max(1, (int) Math.ceil(count / (double) perPage));
        });
    }

    public Optional<PunishmentEntry> getActiveBan(String minecraftName, String minecraftUuid) {
        if (minecraftUuid != null && !minecraftUuid.isBlank()) {
            PunishmentEntry byUuid = activeBanCacheByUuid.get(minecraftUuid.toLowerCase(Locale.ROOT));
            if (byUuid != null) {
                return Optional.of(byUuid);
            }
        }

        if (minecraftName != null && !minecraftName.isBlank()) {
            PunishmentEntry byName = activeBanCacheByName.get(minecraftName.toLowerCase(Locale.ROOT));
            if (byName != null) {
                return Optional.of(byName);
            }
        }

        return Optional.empty();
    }

    public Optional<PunishmentEntry> getActiveMute(String minecraftName, String minecraftUuid) {
        if (minecraftUuid != null && !minecraftUuid.isBlank()) {
            PunishmentEntry byUuid = activeMuteCacheByUuid.get(minecraftUuid.toLowerCase(Locale.ROOT));
            if (byUuid != null) {
                return Optional.of(byUuid);
            }
        }

        if (minecraftName != null && !minecraftName.isBlank()) {
            PunishmentEntry byName = activeMuteCacheByName.get(minecraftName.toLowerCase(Locale.ROOT));
            if (byName != null) {
                return Optional.of(byName);
            }
        }

        return Optional.empty();
    }

    public void processExpiredPunishment(PunishmentEntry entry) {
        if (entry == null || entry.getPunishmentType() == null) {
            return;
        }

        long now = System.currentTimeMillis();

        if (entry.getPunishmentType() == PunishmentType.BAN) {
            boolean expiredNow = repository.markExpired(entry.getId(), now, "Ban verlopen");
            if (!expiredNow) {
                return;
            }

            removeBan(entry);
            cancelScheduledExpiration(entry.getId());

            logAudit("UNBAN_EXPIRED", null, entry);
            discordPunishmentDMService.sendUnbanDM(entry.getMinecraftName(), "Automatisch", "Ban verlopen", true);
            plugin.getDiscordPunishmentLogger().logUnban(entry, "Automatisch", "Ban verlopen");

            Player player = Bukkit.getPlayerExact(entry.getMinecraftName());
            if (player != null) {
                List<String> lines = PunishmentFormatter.apply(
                        config.getUnbanExpiredMessage(),
                        entry.getMinecraftName(),
                        "Automatisch",
                        "Ban verlopen",
                        null,
                        null,
                        config.getServerName()
                );
                Bukkit.getScheduler().runTask(plugin, () -> lines.forEach(player::sendMessage));
            }
            return;
        }

        if (entry.getPunishmentType() == PunishmentType.MUTE) {
            boolean expiredNow = repository.markExpired(entry.getId(), now, "Mute verlopen");
            if (!expiredNow) {
                return;
            }

            removeMute(entry);
            cancelScheduledExpiration(entry.getId());

            logAudit("UNMUTE_EXPIRED", null, entry);
            discordPunishmentDMService.sendUnmuteDM(entry.getMinecraftName(), "Automatisch", "Mute verlopen");
            plugin.getDiscordPunishmentLogger().logUnmute(entry, "Automatisch", "Mute verlopen");

            Player player = Bukkit.getPlayerExact(entry.getMinecraftName());
            if (player != null) {
                List<String> lines = PunishmentFormatter.apply(
                        config.getUnmutePlayerMessage(),
                        entry.getMinecraftName(),
                        "Automatisch",
                        "Mute verlopen",
                        null,
                        null,
                        null
                );
                Bukkit.getScheduler().runTask(plugin, () -> lines.forEach(player::sendMessage));
            }
        }
    }

    private void scheduleExpiration(PunishmentEntry entry) {
        if (entry.getExpiresAt() == null) {
            return;
        }

        cancelScheduledExpiration(entry.getId());

        long delayMs = entry.getExpiresAt() - System.currentTimeMillis();
        if (delayMs <= 0L) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> processExpiredPunishment(entry));
            return;
        }

        long delayTicks = Math.max(1L, delayMs / 50L);

        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            scheduledExpirations.remove(entry.getId());
            processExpiredPunishment(entry);
        }, delayTicks);

        scheduledExpirations.put(entry.getId(), task);
    }

    private void cancelScheduledExpiration(long id) {
        BukkitTask task = scheduledExpirations.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    private void cacheBan(PunishmentEntry entry) {
        if (entry.getMinecraftName() != null) {
            activeBanCacheByName.put(entry.getMinecraftName().toLowerCase(Locale.ROOT), entry);
        }
        if (entry.getMinecraftUuid() != null) {
            activeBanCacheByUuid.put(entry.getMinecraftUuid().toLowerCase(Locale.ROOT), entry);
        }
    }

    private void cacheMute(PunishmentEntry entry) {
        if (entry.getMinecraftName() != null) {
            activeMuteCacheByName.put(entry.getMinecraftName().toLowerCase(Locale.ROOT), entry);
        }
        if (entry.getMinecraftUuid() != null) {
            activeMuteCacheByUuid.put(entry.getMinecraftUuid().toLowerCase(Locale.ROOT), entry);
        }
    }

    private void removeBan(PunishmentEntry entry) {
        if (entry.getMinecraftName() != null) {
            activeBanCacheByName.remove(entry.getMinecraftName().toLowerCase(Locale.ROOT));
        }
        if (entry.getMinecraftUuid() != null) {
            activeBanCacheByUuid.remove(entry.getMinecraftUuid().toLowerCase(Locale.ROOT));
        }
    }

    private void removeMute(PunishmentEntry entry) {
        if (entry.getMinecraftName() != null) {
            activeMuteCacheByName.remove(entry.getMinecraftName().toLowerCase(Locale.ROOT));
        }
        if (entry.getMinecraftUuid() != null) {
            activeMuteCacheByUuid.remove(entry.getMinecraftUuid().toLowerCase(Locale.ROOT));
        }
    }

    private String normalizeReason(String type, String reason) {
        if (reason == null || reason.isBlank()) {
            return config.getDefaultReason(type);
        }
        return reason;
    }

    private boolean isSelfPunish(CommandSender sender, ResolvedTarget target) {
        if (config.isSelfPunishmentAllowed()) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            return false;
        }
        return target.minecraftUuid() != null &&
                player.getUniqueId().toString().equalsIgnoreCase(target.minecraftUuid());
    }

    private PunishmentEntry createBaseEntry(CommandSender sender,
                                            ResolvedTarget target,
                                            PunishmentType type,
                                            Long durationMs,
                                            String reason) {
        long now = System.currentTimeMillis();

        PunishmentEntry entry = new PunishmentEntry();
        entry.setDiscordName(target.discordName());
        entry.setDiscordId(target.discordId());
        entry.setMinecraftName(target.minecraftName());
        entry.setMinecraftUuid(target.minecraftUuid());
        entry.setPunishmentType(type);
        entry.setReason(reason);
        entry.setIssuedByName(getActorName(sender));
        entry.setIssuedByUuid(getActorUuid(sender));
        entry.setIssuedByDiscordName(null);
        entry.setIssuedByDiscordId(null);
        entry.setIssuedSource(getActorSource(sender));
        entry.setDurationMs(durationMs == null || durationMs <= 0L ? null : durationMs);
        entry.setIssuedAt(now);
        entry.setExpiresAt(durationMs == null || durationMs <= 0L ? null : now + durationMs);
        entry.setStatus(type == PunishmentType.KICK ? PunishmentStatus.REMOVED : PunishmentStatus.ACTIVE);
        entry.setServerName(config.getServerName());
        entry.setNotifyOnJoin(config.isOfflineNotificationsEnabled() && (type == PunishmentType.WARN || type == PunishmentType.MUTE));
        entry.setNotificationDelivered(false);

        return entry;
    }

    private ResolvedTarget resolveTarget(String inputName) {
        String lookupName = inputName == null ? "" : inputName.trim();

        String minecraftName = lookupName;
        String minecraftUuid = null;

        Player online = Bukkit.getPlayerExact(lookupName);
        if (online != null) {
            minecraftName = online.getName();
            minecraftUuid = online.getUniqueId().toString();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(lookupName);
            if (offline.getName() != null && !offline.getName().isBlank()) {
                minecraftName = offline.getName();
            }
            if (offline.getUniqueId() != null) {
                minecraftUuid = offline.getUniqueId().toString();
            }
        }

        Optional<DiscordMinecraftLink> link = linkService.findByMinecraftName(minecraftName);

        String discordId = link.map(DiscordMinecraftLink::getDiscordUserId).orElse(null);
        String discordName = link.map(DiscordMinecraftLink::getDiscordTag).orElse(null);

        if (link.isPresent()) {
            if (minecraftUuid == null || minecraftUuid.isBlank()) {
                minecraftUuid = link.get().getMinecraftUuid();
            }
            if (minecraftName == null || minecraftName.isBlank()) {
                minecraftName = link.get().getMinecraftName();
            }
        }

        return new ResolvedTarget(discordName, discordId, minecraftName, minecraftUuid);
    }

    private void kickIfOnline(String minecraftName, List<String> lines) {
        Player target = Bukkit.getPlayerExact(minecraftName);
        if (target == null) {
            return;
        }
        String message = String.join("\n", lines);
        target.kickPlayer(message);
    }

    private void notifyPlayerIfOnline(String minecraftName, List<String> template, PunishmentEntry entry) {
        Player target = Bukkit.getPlayerExact(minecraftName);
        if (target == null) {
            return;
        }

        List<String> lines = PunishmentFormatter.apply(
                template,
                entry.getMinecraftName(),
                entry.getIssuedByName(),
                entry.getReason(),
                PunishmentFormatter.formatDuration(entry.getDurationMs()),
                PunishmentFormatter.formatDate(entry.getExpiresAt(), config.getDateFormat(), config.getTimezone()),
                null
        );

        lines.forEach(target::sendMessage);
    }

    private void notifySimplePlayerIfOnline(String minecraftName, List<String> template, String actor, String reason) {
        Player target = Bukkit.getPlayerExact(minecraftName);
        if (target == null) {
            return;
        }

        List<String> lines = PunishmentFormatter.apply(template, minecraftName, actor, reason, null, null, null);
        lines.forEach(target::sendMessage);
    }

    private List<String> buildBanDisconnectMessage(PunishmentEntry entry) {
        return PunishmentFormatter.apply(
                config.getBanDisconnectMessage(),
                entry.getMinecraftName(),
                entry.getIssuedByName(),
                entry.getReason(),
                PunishmentFormatter.formatDuration(entry.getDurationMs()),
                PunishmentFormatter.formatDate(entry.getExpiresAt(), config.getDateFormat(), config.getTimezone()),
                null
        );
    }

    private List<String> buildKickDisconnectMessage(PunishmentEntry entry) {
        return PunishmentFormatter.apply(
                config.getKickDisconnectMessage(),
                entry.getMinecraftName(),
                entry.getIssuedByName(),
                entry.getReason(),
                null,
                null,
                null
        );
    }

    private void broadcastStaff(String template, PunishmentEntry entry) {
        broadcastStaff(template, entry, entry.getIssuedByName(), entry.getReason());
    }

    private void broadcastStaff(String template, PunishmentEntry entry, String actor, String reason) {
        String message = PunishmentFormatter.apply(
                template,
                entry.getMinecraftName(),
                actor,
                reason,
                PunishmentFormatter.formatDuration(entry.getDurationMs()),
                PunishmentFormatter.formatDate(entry.getExpiresAt(), config.getDateFormat(), config.getTimezone()),
                null
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("lekkeradmin.admin") || player.hasPermission("lekkeradmin.punishment.notify")) {
                player.sendMessage(message);
            }
        }
    }

    private void logAudit(String actionType, CommandSender sender, PunishmentEntry entry) {
        AuditLogEntry logEntry = new AuditLogEntry();
        logEntry.setActionType(actionType);

        if (sender != null) {
            logEntry.setActorId(getActorUuid(sender));
            logEntry.setActorName(getActorName(sender));
        } else {
            logEntry.setActorId(null);
            logEntry.setActorName("SYSTEM");
        }

        logEntry.setTargetId(entry.getMinecraftUuid());
        logEntry.setTargetName(entry.getMinecraftName());
        logEntry.setApplicationId(null);
        logEntry.setDetails(actionType + " -> " + entry.getPunishmentType().name() + " | reason=" + entry.getReason());
        logEntry.setCreatedAt(System.currentTimeMillis());

        auditLogRepository.save(logEntry);
    }

    private String getActorName(CommandSender sender) {
        return sender.getName();
    }

    private String getActorUuid(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId().toString();
        }
        return null;
    }

    private PunishmentSource getActorSource(CommandSender sender) {
        if (sender instanceof Player) {
            return PunishmentSource.MINECRAFT;
        }
        return PunishmentSource.CONSOLE;
    }

    public record Result(boolean success, String message, PunishmentEntry entry) {
        public static Result success(PunishmentEntry entry) {
            return new Result(true, null, entry);
        }

        public static Result fail(String message) {
            return new Result(false, message, null);
        }
    }

    private record ResolvedTarget(String discordName, String discordId, String minecraftName, String minecraftUuid) {
    }
}