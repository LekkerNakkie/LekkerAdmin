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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class WarnService {

    private final LekkerAdmin plugin;
    private final PunishmentsConfig config;
    private final PunishmentRepository repository;
    private final AuditLogRepository auditLogRepository;
    private final LinkService linkService;
    private final DiscordPunishmentDMService discordPunishmentDMService;

    public WarnService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getPunishmentsConfig();
        this.repository = new PunishmentRepository(plugin);
        this.auditLogRepository = new AuditLogRepository(plugin);
        this.linkService = new LinkService(plugin);
        this.discordPunishmentDMService = new DiscordPunishmentDMService(plugin);
    }

    public CompletableFuture<Result> warnAsync(CommandSender sender, String targetName, String reason) {
        return plugin.getDatabaseManager().supplyAsync(() -> {
            ResolvedTarget target = resolveTarget(targetName);

            if (target.minecraftName() == null || target.minecraftName().isBlank()) {
                return Result.fail(config.getPlayerNotFoundMessage());
            }

            if (isSelfPunish(sender, target)) {
                return Result.fail(config.getCannotPunishSelfMessage());
            }

            String finalReason = normalizeReason(reason);
            PunishmentEntry entry = createWarnEntry(sender, target, finalReason);

            repository.createPunishment(entry);

            Bukkit.getScheduler().runTask(plugin, () -> {
                notifyPlayerIfOnline(entry);
                broadcastStaff(entry);
                sender.sendMessage(PunishmentFormatter.apply(
                        config.getWarnSenderMessage(),
                        entry.getMinecraftName(),
                        PunishmentFormatter.valueOrUnknown(entry.getIssuedByName()),
                        PunishmentFormatter.valueOrUnknown(entry.getReason()),
                        null,
                        null,
                        config.getServerName()
                ));
            });

            logAudit("WARN", sender, entry);

            if (config.isDiscordDMEnabled() && config.isDiscordDMEnabled("warn")) {
                discordPunishmentDMService.sendPunishmentDM(entry);
            }

            plugin.getDiscordPunishmentLogger().logWarn(entry);

            return Result.success(entry);
        });
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return config.getDefaultReason("warn");
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

        return target.minecraftUuid() != null
                && player.getUniqueId().toString().equalsIgnoreCase(target.minecraftUuid());
    }

    private PunishmentEntry createWarnEntry(CommandSender sender, ResolvedTarget target, String reason) {
        long now = System.currentTimeMillis();

        PunishmentEntry entry = new PunishmentEntry();
        entry.setDiscordName(target.discordName());
        entry.setDiscordId(target.discordId());
        entry.setMinecraftName(target.minecraftName());
        entry.setMinecraftUuid(target.minecraftUuid());
        entry.setPunishmentType(PunishmentType.WARN);
        entry.setReason(reason);
        entry.setIssuedByName(getActorName(sender));
        entry.setIssuedByUuid(getActorUuid(sender));
        entry.setIssuedByDiscordName(null);
        entry.setIssuedByDiscordId(null);
        entry.setIssuedSource(getActorSource(sender));
        entry.setDurationMs(null);
        entry.setIssuedAt(now);
        entry.setExpiresAt(null);
        entry.setStatus(PunishmentStatus.REMOVED);
        entry.setRemovedAt(now);
        entry.setRemoveReason(reason);
        entry.setServerName(config.getServerName());
        entry.setNotifyOnJoin(config.isOfflineNotificationsEnabled());
        entry.setNotificationDelivered(false);

        return entry;
    }

    private void notifyPlayerIfOnline(PunishmentEntry entry) {
        Player player = Bukkit.getPlayerExact(entry.getMinecraftName());
        if (player == null) {
            return;
        }

        String date = PunishmentFormatter.formatDate(
                entry.getIssuedAt(),
                config.getDateFormat(),
                config.getTimezone()
        );

        List<String> lines = PunishmentFormatter.apply(
                config.getWarnPlayerMessage(),
                entry.getMinecraftName(),
                PunishmentFormatter.valueOrUnknown(entry.getIssuedByName()),
                PunishmentFormatter.valueOrUnknown(entry.getReason()),
                null,
                null,
                config.getServerName(),
                date
        );

        lines.forEach(player::sendMessage);
    }

    private void broadcastStaff(PunishmentEntry entry) {
        String message = PunishmentFormatter.apply(
                config.getWarnStaffMessage(),
                entry.getMinecraftName(),
                PunishmentFormatter.valueOrUnknown(entry.getIssuedByName()),
                PunishmentFormatter.valueOrUnknown(entry.getReason()),
                null,
                null,
                config.getServerName()
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
        logEntry.setActorId(getActorUuid(sender));
        logEntry.setActorName(getActorName(sender));
        logEntry.setTargetId(entry.getMinecraftUuid());
        logEntry.setTargetName(entry.getMinecraftName());
        logEntry.setApplicationId(null);
        logEntry.setDetails(actionType + " -> " + entry.getPunishmentType().name() + " | reason=" + entry.getReason());
        logEntry.setCreatedAt(System.currentTimeMillis());

        auditLogRepository.save(logEntry);
    }

    private ResolvedTarget resolveTarget(String inputName) {
        String name = inputName == null ? "" : inputName.trim();

        Optional<DiscordMinecraftLink> link = linkService.findByMinecraftName(name);

        String discordId = link.map(DiscordMinecraftLink::getDiscordUserId).orElse(null);
        String discordName = link.map(DiscordMinecraftLink::getDiscordTag).orElse(null);
        String minecraftUuid = link.map(DiscordMinecraftLink::getMinecraftUuid).orElse(null);
        String minecraftName = link.map(DiscordMinecraftLink::getMinecraftName).orElse(name);

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            minecraftName = online.getName();
            minecraftUuid = online.getUniqueId().toString();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            if (offline.getName() != null && !offline.getName().isBlank()) {
                minecraftName = offline.getName();
            }
            if (offline.getUniqueId() != null && minecraftUuid == null) {
                minecraftUuid = offline.getUniqueId().toString();
            }
        }

        return new ResolvedTarget(discordName, discordId, minecraftName, minecraftUuid);
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