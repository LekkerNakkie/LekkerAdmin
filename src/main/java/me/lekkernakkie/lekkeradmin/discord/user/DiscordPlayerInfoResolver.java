package me.lekkernakkie.lekkeradmin.discord.user;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.repository.PunishmentRepository;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentStatus;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentType;
import me.lekkernakkie.lekkeradmin.service.application.ApplicationService;
import me.lekkernakkie.lekkeradmin.service.link.LinkLookupService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiscordPlayerInfoResolver {

    private final LinkLookupService linkLookupService;
    private final ApplicationService applicationService;
    private final PunishmentRepository punishmentRepository;

    public DiscordPlayerInfoResolver(LekkerAdmin plugin) {
        this.linkLookupService = new LinkLookupService(plugin);
        this.applicationService = new ApplicationService(plugin);
        this.punishmentRepository = new PunishmentRepository(plugin);
    }

    public Optional<PlayerInfoResult> resolve(String query, int activeLimit, int expiredLimit) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        String trimmed = query.trim();

        Optional<DiscordMinecraftLink> byDiscordId = linkLookupService.findByDiscordUserId(trimmed);
        if (byDiscordId.isPresent()) {
            return Optional.of(buildFromLink(byDiscordId.get(), activeLimit, expiredLimit));
        }

        Optional<DiscordMinecraftLink> byMinecraftName = linkLookupService.findByMinecraftName(trimmed);
        if (byMinecraftName.isPresent()) {
            return Optional.of(buildFromLink(byMinecraftName.get(), activeLimit, expiredLimit));
        }

        Optional<WhitelistApplication> appByDiscord = applicationService.findByDiscordUserId(trimmed);
        if (appByDiscord.isPresent()) {
            return Optional.of(buildFromApplication(appByDiscord.get(), activeLimit, expiredLimit));
        }

        Optional<WhitelistApplication> appByMinecraft = applicationService.findByMinecraftName(trimmed);
        if (appByMinecraft.isPresent()) {
            return Optional.of(buildFromApplication(appByMinecraft.get(), activeLimit, expiredLimit));
        }

        return Optional.empty();
    }

    private PlayerInfoResult buildFromLink(DiscordMinecraftLink link, int activeLimit, int expiredLimit) {
        Optional<WhitelistApplication> application = link.getApplicationId() == null
                ? Optional.empty()
                : applicationService.findByApplicationId(link.getApplicationId());

        List<PunishmentEntry> history = punishmentRepository.findRecentPunishmentsByMinecraftName(
                link.getMinecraftName(),
                Math.max(activeLimit + expiredLimit + 10, 20)
        );

        return buildResult(
                link.getDiscordTag(),
                link.getDiscordUserId(),
                link.getMinecraftName(),
                link.getMinecraftUuid(),
                true,
                application.orElse(null),
                history,
                activeLimit,
                expiredLimit
        );
    }

    private PlayerInfoResult buildFromApplication(WhitelistApplication application, int activeLimit, int expiredLimit) {
        List<PunishmentEntry> history = new ArrayList<>();

        if (application.getMinecraftName() != null && !application.getMinecraftName().isBlank()) {
            history = punishmentRepository.findRecentPunishmentsByMinecraftName(
                    application.getMinecraftName(),
                    Math.max(activeLimit + expiredLimit + 10, 20)
            );
        } else if (application.getDiscordUserId() != null && !application.getDiscordUserId().isBlank()) {
            history = punishmentRepository.findRecentPunishmentsByDiscordId(
                    application.getDiscordUserId(),
                    Math.max(activeLimit + expiredLimit + 10, 20)
            );
        }

        boolean linked = application.getMinecraftUuid() != null && !application.getMinecraftUuid().isBlank();

        return buildResult(
                application.getDiscordTag(),
                application.getDiscordUserId(),
                application.getMinecraftName(),
                application.getMinecraftUuid(),
                linked,
                application,
                history,
                activeLimit,
                expiredLimit
        );
    }

    private PlayerInfoResult buildResult(String discordName,
                                         String discordId,
                                         String minecraftName,
                                         String minecraftUuid,
                                         boolean linked,
                                         WhitelistApplication application,
                                         List<PunishmentEntry> history,
                                         int activeLimit,
                                         int expiredLimit) {

        List<PunishmentEntry> active = new ArrayList<>();
        List<PunishmentEntry> expired = new ArrayList<>();

        for (PunishmentEntry entry : history) {
            if (entry == null || entry.getStatus() == null || entry.getPunishmentType() == null) {
                continue;
            }

            PunishmentStatus status = entry.getStatus();
            PunishmentType type = entry.getPunishmentType();

            if (status == PunishmentStatus.ACTIVE) {
                if (active.size() < activeLimit) {
                    active.add(entry);
                }
                continue;
            }

            if (status == PunishmentStatus.EXPIRED) {
                if (expired.size() < expiredLimit) {
                    expired.add(entry);
                }
                continue;
            }

            if (status == PunishmentStatus.REMOVED) {
                if (type == PunishmentType.WARN || type == PunishmentType.KICK) {
                    if (expired.size() < expiredLimit) {
                        expired.add(entry);
                    }
                }
            }
        }

        String whitelistedBy = null;
        if (application != null
                && application.getReviewedByDiscordId() != null
                && !application.getReviewedByDiscordId().isBlank()) {
            whitelistedBy = application.getReviewedByDiscordId();
        }

        return new PlayerInfoResult(
                discordName,
                discordId,
                minecraftName,
                minecraftUuid,
                linked,
                application != null,
                application == null ? null : application.getStatus() == null ? null : application.getStatus().name(),
                whitelistedBy,
                active,
                expired,
                active.size() + expired.size()
        );
    }

    public record PlayerInfoResult(
            String discordName,
            String discordId,
            String minecraftName,
            String minecraftUuid,
            boolean linked,
            boolean hasApplication,
            String applicationStatus,
            String whitelistedByDiscordId,
            List<PunishmentEntry> activePunishments,
            List<PunishmentEntry> expiredPunishments,
            int totalPunishments
    ) {
    }
}