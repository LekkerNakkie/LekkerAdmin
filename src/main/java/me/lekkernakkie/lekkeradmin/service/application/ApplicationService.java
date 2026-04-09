package me.lekkernakkie.lekkeradmin.service.application;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.repository.ApplicationRepository;
import me.lekkernakkie.lekkeradmin.model.application.ApplicationStatus;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;

import java.util.Optional;
import java.util.UUID;

public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationService(LekkerAdmin plugin) {
        this.repository = new ApplicationRepository(plugin);
    }

    public WhitelistApplication createDraft(String discordUserId, String discordTag) {
        WhitelistApplication application = new WhitelistApplication();
        application.setApplicationId(UUID.randomUUID().toString().replace("-", ""));
        application.setDiscordUserId(discordUserId);
        application.setDiscordTag(discordTag);
        application.setStatus(ApplicationStatus.PENDING_NAME_VALIDATION);
        application.setSubmittedAt(System.currentTimeMillis());
        application.setNameRetryCount(0);
        return application;
    }

    public void saveNewApplication(WhitelistApplication application) {
        repository.saveApplication(application);
    }

    public void update(WhitelistApplication application) {
        repository.updateApplication(application);
    }

    public Optional<WhitelistApplication> findByApplicationId(String applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    public Optional<WhitelistApplication> findLatestApplicationByDiscordUserId(String discordUserId) {
        return repository.findLatestByDiscordUserId(discordUserId);
    }

    public Optional<WhitelistApplication> findLatestOpenApplicationByDiscordUserId(String discordUserId) {
        return repository.findLatestOpenByDiscordUserId(discordUserId);
    }

    public Optional<WhitelistApplication> findByDiscordUserId(String discordUserId) {
        return repository.findByDiscordUserId(discordUserId);
    }

    public Optional<WhitelistApplication> findByMinecraftName(String minecraftName) {
        return repository.findByMinecraftName(minecraftName);
    }

    public Optional<WhitelistApplication> findByMinecraftUuid(String minecraftUuid) {
        return repository.findByMinecraftUuid(minecraftUuid);
    }

    public void markApproved(WhitelistApplication application, String reviewerDiscordId, String reason) {
        markApproved(application, reviewerDiscordId, null, reason);
    }

    public void markApproved(WhitelistApplication application, String reviewerDiscordId, String reviewerDiscordName, String reason) {
        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedByDiscordId(reviewerDiscordId);
        application.setReviewedByDiscordName(reviewerDiscordName);
        application.setReviewReason(reason);
        application.setReviewedAt(System.currentTimeMillis());
        repository.updateApplication(application);
    }

    public void markDenied(WhitelistApplication application, String reviewerDiscordId, String reason) {
        markDenied(application, reviewerDiscordId, null, reason);
    }

    public void markDenied(WhitelistApplication application, String reviewerDiscordId, String reviewerDiscordName, String reason) {
        application.setStatus(ApplicationStatus.DENIED);
        application.setReviewedByDiscordId(reviewerDiscordId);
        application.setReviewedByDiscordName(reviewerDiscordName);
        application.setReviewReason(reason);
        application.setReviewedAt(System.currentTimeMillis());
        repository.updateApplication(application);
    }

    public void markApprovedPendingUsernameFix(WhitelistApplication application, String reviewerDiscordId, String reason) {
        markApprovedPendingUsernameFix(application, reviewerDiscordId, null, reason);
    }

    public void markApprovedPendingUsernameFix(WhitelistApplication application, String reviewerDiscordId, String reviewerDiscordName, String reason) {
        application.setStatus(ApplicationStatus.APPROVED_PENDING_NAME_FIX);
        application.setReviewedByDiscordId(reviewerDiscordId);
        application.setReviewedByDiscordName(reviewerDiscordName);
        application.setReviewReason(reason);
        application.setReviewedAt(System.currentTimeMillis());
        repository.updateApplication(application);
    }

    public void markCompleted(WhitelistApplication application) {
        long now = System.currentTimeMillis();
        application.setStatus(ApplicationStatus.COMPLETED);
        application.setLinkedAt(now);
        application.setFinalizedAt(now);
        repository.updateApplication(application);
    }
}