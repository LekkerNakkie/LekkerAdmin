package me.lekkernakkie.lekkeradmin.service.whitelist;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.model.application.ApplicationStatus;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.service.application.ApplicationService;
import me.lekkernakkie.lekkeradmin.service.link.LinkService;

import java.util.Optional;

public class WhitelistService {

    private final DCBotConfig config;
    private final ApplicationService applicationService;
    private final LinkService linkService;
    private final MinecraftWhitelistService minecraftWhitelistService;
    private final UsernameValidationService usernameValidationService;

    public WhitelistService(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.applicationService = new ApplicationService(plugin);
        this.linkService = new LinkService(plugin);
        this.minecraftWhitelistService = new MinecraftWhitelistService(plugin);
        this.usernameValidationService = new UsernameValidationService(plugin);
    }

    public WhitelistResult finalizeApprovedApplication(WhitelistApplication application) {
        return finalizeApprovedApplication(application, null, null);
    }

    public WhitelistResult finalizeApprovedApplication(WhitelistApplication application, String reviewerDiscordId, String reviewerDiscordName) {
        if (application == null) {
            return new WhitelistResult(false, false, "Application is null.");
        }

        if (reviewerDiscordId != null && !reviewerDiscordId.isBlank()) {
            application.setReviewedByDiscordId(reviewerDiscordId);
        }

        if (reviewerDiscordName != null && !reviewerDiscordName.isBlank()) {
            application.setReviewedByDiscordName(reviewerDiscordName);
        }

        if (application.getReviewedAt() == null) {
            application.setReviewedAt(System.currentTimeMillis());
        }

        if (application.getMinecraftName() == null || application.getMinecraftName().isBlank()) {
            return new WhitelistResult(false, true, "Minecraft naam ontbreekt.");
        }

        UsernameValidationService.ValidationResult validation =
                usernameValidationService.validate(application.getMinecraftName());

        if (!validation.valid()) {
            applicationService.markApprovedPendingUsernameFix(
                    application,
                    application.getReviewedByDiscordId(),
                    application.getReviewedByDiscordName(),
                    application.getReviewReason()
            );
            return new WhitelistResult(false, true, validation.reason());
        }

        String resolvedUuid = application.getMinecraftUuid();
        if (resolvedUuid == null || resolvedUuid.isBlank()) {
            resolvedUuid = validation.minecraftUuid();
        }

        if (resolvedUuid == null || resolvedUuid.isBlank()) {
            applicationService.markApprovedPendingUsernameFix(
                    application,
                    application.getReviewedByDiscordId(),
                    application.getReviewedByDiscordName(),
                    application.getReviewReason()
            );
            return new WhitelistResult(false, true, "UUID ontbreekt voor deze Minecraft naam.");
        }

        application.setMinecraftUuid(resolvedUuid);

        Optional<DiscordMinecraftLink> existingDiscordLink =
                linkService.findByDiscordUserId(application.getDiscordUserId());

        if (existingDiscordLink.isPresent()
                && !existingDiscordLink.get().getMinecraftUuid().equalsIgnoreCase(resolvedUuid)) {
            return new WhitelistResult(false, false, "Discord account is al gekoppeld aan een ander Minecraft account.");
        }

        Optional<DiscordMinecraftLink> existingMinecraftLink =
                linkService.findByMinecraftUuid(resolvedUuid);

        if (existingMinecraftLink.isEmpty()) {
            existingMinecraftLink = linkService.findByMinecraftName(application.getMinecraftName());
        }

        if (existingMinecraftLink.isPresent()) {
            boolean sameDiscord = existingMinecraftLink.get().getDiscordUserId().equalsIgnoreCase(application.getDiscordUserId());
            boolean sameUuid = existingMinecraftLink.get().getMinecraftUuid() != null
                    && existingMinecraftLink.get().getMinecraftUuid().equalsIgnoreCase(resolvedUuid);

            if (!sameDiscord || !sameUuid) {
                return new WhitelistResult(false, false, "Minecraft account is al gekoppeld aan een ander Discord account.");
            }
        }

        boolean alreadyWhitelisted = minecraftWhitelistService.isWhitelisted(application.getMinecraftName(), config.isCaseSensitiveNameCheck());

        if (!alreadyWhitelisted) {
            boolean whitelisted = minecraftWhitelistService.addToWhitelist(application.getMinecraftName());
            if (!whitelisted) {
                return new WhitelistResult(false, false, "Could not add player to the Minecraft whitelist.");
            }
        }

        if (config.isAutoLinkOnApprove() && existingDiscordLink.isEmpty()) {
            linkService.link(
                    application.getDiscordUserId(),
                    application.getDiscordTag(),
                    resolvedUuid,
                    application.getMinecraftName(),
                    application.getApplicationId()
            );
        }

        if (application.getStatus() != ApplicationStatus.COMPLETED) {
            applicationService.markCompleted(application);
        }

        if (alreadyWhitelisted) {
            return new WhitelistResult(true, false, "Player stond al op de whitelist en is nu correct afgewerkt.");
        }

        return new WhitelistResult(true, false, "Whitelist completed successfully.");
    }

    public record WhitelistResult(boolean success, boolean needsNameFix, String message) {
    }
}