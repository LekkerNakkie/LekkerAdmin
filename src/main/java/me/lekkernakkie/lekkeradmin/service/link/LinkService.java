package me.lekkernakkie.lekkeradmin.service.link;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.application.ApplicationStatus;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.service.application.ApplicationService;

import java.util.Optional;

public class LinkService {

    private final ApplicationService applicationService;

    public LinkService(LekkerAdmin plugin) {
        this.applicationService = new ApplicationService(plugin);
    }

    public void link(String discordUserId,
                     String discordTag,
                     String minecraftUuid,
                     String minecraftName,
                     String applicationId) {

        Optional<WhitelistApplication> optional = applicationService.findByApplicationId(applicationId);
        if (optional.isEmpty()) {
            throw new RuntimeException("Could not link accounts, application not found: " + applicationId);
        }

        WhitelistApplication application = optional.get();
        application.setDiscordUserId(discordUserId);
        application.setDiscordTag(discordTag);
        application.setMinecraftUuid(minecraftUuid);
        application.setMinecraftName(minecraftName);
        application.setLinkedAt(System.currentTimeMillis());

        applicationService.update(application);
    }

    public Optional<DiscordMinecraftLink> findByDiscordUserId(String discordUserId) {
        return applicationService.findByDiscordUserId(discordUserId)
                .filter(this::isActiveLink)
                .map(this::mapLink);
    }

    public Optional<DiscordMinecraftLink> findByMinecraftName(String minecraftName) {
        return applicationService.findByMinecraftName(minecraftName)
                .filter(this::isActiveLink)
                .map(this::mapLink);
    }

    public Optional<DiscordMinecraftLink> mapIfActive(WhitelistApplication application) {
        if (application == null || !isActiveLink(application)) {
            return Optional.empty();
        }
        return Optional.of(mapLink(application));
    }

    private boolean isActiveLink(WhitelistApplication application) {
        return application.getStatus() == ApplicationStatus.COMPLETED
                && application.getMinecraftUuid() != null
                && !application.getMinecraftUuid().isBlank()
                && application.getLinkedAt() != null;
    }

    private DiscordMinecraftLink mapLink(WhitelistApplication application) {
        DiscordMinecraftLink link = new DiscordMinecraftLink();
        link.setDiscordUserId(application.getDiscordUserId());
        link.setDiscordTag(application.getDiscordTag());
        link.setMinecraftUuid(application.getMinecraftUuid());
        link.setMinecraftName(application.getMinecraftName());
        link.setApplicationId(application.getApplicationId());
        link.setLinkedAt(application.getLinkedAt());
        return link;
    }
}