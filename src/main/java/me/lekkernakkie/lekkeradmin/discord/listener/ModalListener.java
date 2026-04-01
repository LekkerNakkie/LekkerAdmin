package me.lekkernakkie.lekkeradmin.discord.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.discord.interaction.component.ComponentIdParser;
import me.lekkernakkie.lekkeradmin.discord.log.DiscordWebhookLogger;
import me.lekkernakkie.lekkeradmin.discord.message.DiscordDmService;
import me.lekkernakkie.lekkeradmin.discord.message.DiscordStaffReviewService;
import me.lekkernakkie.lekkeradmin.discord.user.DiscordRoleService;
import me.lekkernakkie.lekkeradmin.model.application.ApplicationAnswer;
import me.lekkernakkie.lekkeradmin.model.application.ApplicationStatus;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.service.application.ApplicationService;
import me.lekkernakkie.lekkeradmin.service.application.NameRetryService;
import me.lekkernakkie.lekkeradmin.service.audit.AuditLogService;
import me.lekkernakkie.lekkeradmin.service.link.LinkService;
import me.lekkernakkie.lekkeradmin.service.whitelist.MinecraftWhitelistService;
import me.lekkernakkie.lekkeradmin.service.whitelist.UsernameValidationService;
import me.lekkernakkie.lekkeradmin.service.whitelist.WhitelistService;
import me.lekkernakkie.lekkeradmin.whitelist.validator.ApplicationValidator;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ModalListener extends ListenerAdapter {

    private static final Map<String, Long> LAST_WHITELIST_SUBMISSION_TIMES = new ConcurrentHashMap<>();

    private final LekkerAdmin plugin;
    private final DCBotConfig config;
    private final ApplicationService applicationService;
    private final NameRetryService nameRetryService;
    private final DiscordStaffReviewService discordStaffReviewService;
    private final DiscordDmService discordDmService;
    private final AuditLogService auditLogService;
    private final ApplicationValidator applicationValidator;
    private final WhitelistService whitelistService;
    private final DiscordWebhookLogger webhookLogger;
    private final DiscordRoleService discordRoleService;
    private final UsernameValidationService usernameValidationService;
    private final LinkService linkService;
    private final MinecraftWhitelistService minecraftWhitelistService;

    public ModalListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.applicationService = new ApplicationService(plugin);
        this.nameRetryService = new NameRetryService(plugin);
        this.discordStaffReviewService = new DiscordStaffReviewService(plugin);
        this.discordDmService = new DiscordDmService(plugin);
        this.auditLogService = new AuditLogService(plugin);
        this.applicationValidator = new ApplicationValidator();
        this.whitelistService = new WhitelistService(plugin);
        this.webhookLogger = new DiscordWebhookLogger(plugin);
        this.discordRoleService = new DiscordRoleService(plugin);
        this.usernameValidationService = new UsernameValidationService(plugin);
        this.linkService = new LinkService(plugin);
        this.minecraftWhitelistService = new MinecraftWhitelistService(plugin);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();
        ComponentIdParser.ParsedComponentId parsed = ComponentIdParser.parse(modalId);

        if (parsed == null) {
            return;
        }

        String root = parsed.root();
        String action = parsed.action();

        if (root.equalsIgnoreCase("review")) {
            Member member = event.getMember();
            if (!discordRoleService.hasReviewPermission(member)) {
                event.reply("Je hebt geen permissie om aanvragen te reviewen.")
                        .setEphemeral(true)
                        .queue();
                return;
            }
        }

        if (root.equalsIgnoreCase("whitelist") && action.equalsIgnoreCase("application")) {
            handleWhitelistApplicationSubmit(event);
            return;
        }

        if (root.equalsIgnoreCase("whitelist") && action.equalsIgnoreCase("retryname")) {
            handleRetryNameSubmit(event, parsed.value());
            return;
        }

        if (root.equalsIgnoreCase("review") && action.equalsIgnoreCase("approvereason")) {
            handleApproveReasonSubmit(event, parsed.value());
            return;
        }

        if (root.equalsIgnoreCase("review") && action.equalsIgnoreCase("denyreason")) {
            handleDenyReasonSubmit(event, parsed.value());
        }
    }

    private void handleWhitelistApplicationSubmit(ModalInteractionEvent event) {
        String discordUserId = event.getUser().getId();

        if (isOnWhitelistCooldown(discordUserId)) {
            event.reply("Je moet even wachten voor je opnieuw een whitelist aanvraag kan indienen.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!config.isAllowMultipleOpenApplications()) {
            Optional<WhitelistApplication> openApplication =
                    applicationService.findLatestOpenApplicationByDiscordUserId(discordUserId);

            if (openApplication.isPresent() && !isApplicationExpired(openApplication.get())) {
                event.reply(config.getDmBlockedOpenApplication())
                        .setEphemeral(true)
                        .queue();
                return;
            }
        }

        Optional<DiscordMinecraftLink> existingDiscordLink = linkService.findByDiscordUserId(discordUserId);
        if (existingDiscordLink.isPresent()) {
            event.reply(config.getDmBlockedDiscordAlreadyLinked())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        WhitelistApplication application = applicationService.createDraft(
                discordUserId,
                event.getUser().getAsTag()
        );

        List<ApplicationAnswer> answers = new ArrayList<>();

        event.getValues().forEach(mapping -> {
            String rawId = mapping.getId();
            String value = mapping.getAsString();

            String fieldKey = rawId.startsWith("field:") ? rawId.substring("field:".length()) : rawId;
            String fieldLabel = config.getApplicationFieldLabel(fieldKey);

            if (fieldKey.equalsIgnoreCase("minecraft_name")) {
                application.setMinecraftName(value);
            }

            answers.add(new ApplicationAnswer(
                    fieldKey,
                    fieldLabel,
                    value,
                    answers.size() + 1
            ));
        });

        answers.sort(Comparator.comparingInt(ApplicationAnswer::getFieldOrder));
        application.setAnswers(answers);
        application.setStatus(ApplicationStatus.PENDING_REVIEW);
        application.setSubmittedAt(System.currentTimeMillis());

        ApplicationValidator.ValidationResult validation = applicationValidator.validate(application);
        if (!validation.valid()) {
            event.reply("Je formulier is ongeldig: " + validation.message())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (minecraftWhitelistService.isWhitelisted(application.getMinecraftName(), config.isCaseSensitiveNameCheck())) {
            event.reply(config.getDmBlockedAlreadyWhitelisted())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Optional<DiscordMinecraftLink> existingMinecraftLink = linkService.findByMinecraftName(application.getMinecraftName());
        if (existingMinecraftLink.isPresent()
                && !existingMinecraftLink.get().getDiscordUserId().equalsIgnoreCase(discordUserId)) {
            event.reply(config.getDmBlockedMinecraftAlreadyLinked())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        UsernameValidationService.ValidationResult mcValidation =
                usernameValidationService.validate(application.getMinecraftName());

        if (!mcValidation.valid()) {
            event.reply("De Minecraft naam is ongeldig: " + mcValidation.reason())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        application.setMinecraftUuid(mcValidation.minecraftUuid());

        applicationService.saveNewApplication(application);
        markWhitelistSubmitted(discordUserId);

        auditLogService.log(
                "APPLICATION_SUBMITTED",
                event.getUser().getId(),
                event.getUser().getAsTag(),
                event.getUser().getId(),
                application.getMinecraftName(),
                application.getApplicationId(),
                "Whitelist application submitted"
        );

        discordStaffReviewService.postApplicationReview(application);
        discordDmService.sendSubmittedOverviewDm(application.getDiscordUserId(), application);
        notifyIngameWhitelistRequest(application);

        event.deferReply(true).queue(hook ->
                hook.deleteOriginal().queue(
                        success -> {},
                        error -> {}
                )
        );
    }

    private void handleRetryNameSubmit(ModalInteractionEvent event, String applicationId) {
        Optional<WhitelistApplication> optionalApplication =
                applicationService.findByApplicationId(applicationId);

        if (optionalApplication.isEmpty()) {
            event.reply("Applicatie niet gevonden.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        WhitelistApplication application = optionalApplication.get();

        if (isRetryExpired(application)) {
            event.reply(config.getDmApplicationCancelled())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String newMinecraftName = event.getValue("field:minecraft_name").getAsString();
        String oldName = application.getMinecraftName();

        if (application.getNameRetryCount() >= config.getMaxNameRetries()) {
            event.reply(config.getDmRetryNameFailed())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (minecraftWhitelistService.isWhitelisted(newMinecraftName, config.isCaseSensitiveNameCheck())) {
            event.reply(config.getDmBlockedAlreadyWhitelisted())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Optional<DiscordMinecraftLink> existingMinecraftLink = linkService.findByMinecraftName(newMinecraftName);
        if (existingMinecraftLink.isPresent()
                && !existingMinecraftLink.get().getDiscordUserId().equalsIgnoreCase(application.getDiscordUserId())) {
            event.reply(config.getDmBlockedMinecraftAlreadyLinked())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        NameRetryService.RetryResult retryResult =
                nameRetryService.processRetry(application, newMinecraftName);

        if (!retryResult.success()) {
            event.reply("Naamcorrectie mislukt: " + retryResult.message())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        UsernameValidationService.ValidationResult mcValidation =
                usernameValidationService.validate(application.getMinecraftName());

        if (!mcValidation.valid()) {
            applicationService.update(application);
            event.reply(config.getDmRetryNameFailed())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        application.setMinecraftUuid(mcValidation.minecraftUuid());
        applicationService.update(application);

        WhitelistService.WhitelistResult whitelistResult =
                whitelistService.finalizeApprovedApplication(application);

        if (!whitelistResult.success()) {
            event.reply("Naam werd aangepast, maar finalisatie mislukte: " + whitelistResult.message())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        discordRoleService.giveApproveRole(application.getDiscordUserId());
        webhookLogger.logRetrySuccess(application, oldName, newMinecraftName);

        discordDmService.sendApprovedOverviewDm(
                application.getDiscordUserId(),
                application,
                null
        );

        discordStaffReviewService.editApprovedReview(application, event.getUser().getAsTag(), null);

        event.reply(config.getDmRetryNameSuccess())
                .setEphemeral(true)
                .queue();
    }

    private void handleApproveReasonSubmit(ModalInteractionEvent event, String applicationId) {
        Optional<WhitelistApplication> optionalApplication =
                applicationService.findByApplicationId(applicationId);

        if (optionalApplication.isEmpty()) {
            event.reply("Applicatie niet gevonden.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String reason = event.getValue("field:reason").getAsString();
        WhitelistApplication application = optionalApplication.get();

        applicationService.markApproved(application, event.getUser().getId(), reason);

        WhitelistService.WhitelistResult result =
                whitelistService.finalizeApprovedApplication(application);

        if (result.success()) {
            discordRoleService.giveApproveRole(application.getDiscordUserId());
            webhookLogger.logApproval(event.getUser().getAsTag(), application);

            discordDmService.sendApprovedOverviewDm(
                    application.getDiscordUserId(),
                    application,
                    reason
            );

            discordStaffReviewService.editApprovedReview(application, event.getUser().getAsTag(), reason);

            event.reply("Applicatie goedgekeurd.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (result.needsNameFix()) {
            webhookLogger.logInvalidName(application);

            if (config.isNotifyPlayerOnNameFailure()) {
                discordDmService.sendInvalidMinecraftNameDm(
                        application.getDiscordUserId(),
                        application.getApplicationId(),
                        application.getMinecraftName(),
                        result.message()
                );
            }

            if (config.isNotifyStaffOnNameFailure()) {
                discordStaffReviewService.editPendingNameFixReview(application, event.getUser().getAsTag(), result.message());
            }

            event.reply("Applicatie goedgekeurd, maar naamcorrectie is nodig: " + result.message())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.reply("Goedkeuring mislukt: " + result.message())
                .setEphemeral(true)
                .queue();
    }

    private void handleDenyReasonSubmit(ModalInteractionEvent event, String applicationId) {
        Optional<WhitelistApplication> optionalApplication =
                applicationService.findByApplicationId(applicationId);

        if (optionalApplication.isEmpty()) {
            event.reply("Applicatie niet gevonden.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String reason = event.getValue("field:reason").getAsString();
        WhitelistApplication application = optionalApplication.get();

        applicationService.markDenied(application, event.getUser().getId(), reason);
        webhookLogger.logDenial(event.getUser().getAsTag(), application, reason);

        discordDmService.sendDeniedOverviewDm(
                application.getDiscordUserId(),
                application,
                reason
        );

        discordStaffReviewService.editDeniedReview(application, event.getUser().getAsTag(), reason);

        event.reply("Applicatie afgekeurd.")
                .setEphemeral(true)
                .queue();
    }

    private void notifyIngameWhitelistRequest(WhitelistApplication application) {
        if (!config.isWhitelistIngameNotifyEnabled()) {
            return;
        }

        String permission = config.getWhitelistIngameNotifyPermission();
        String message = config.getWhitelistIngameNotifyMessage()
                .replace("{discord}", application.getDiscordTag() == null ? "-" : application.getDiscordTag())
                .replace("{minecraft}", application.getMinecraftName() == null ? "-" : application.getMinecraftName())
                .replace("{application_id}", application.getApplicationId() == null ? "-" : application.getApplicationId());

        Sound sound = parseSound(config.getWhitelistIngameNotifySound());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
                continue;
            }

            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));

            if (sound != null) {
                player.playSound(player.getLocation(), sound, config.getWhitelistIngameNotifyVolume(), config.getWhitelistIngameNotifyPitch());
            }
        }
    }

    private Sound parseSound(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        try {
            return Sound.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid whitelist ingame notify sound in DCBot.yml: " + input);
            return null;
        }
    }

    private boolean isOnWhitelistCooldown(String discordUserId) {
        int cooldownSeconds = config.getWhitelistCommandCooldownSeconds();
        if (cooldownSeconds <= 0) {
            return false;
        }

        Long lastSubmission = LAST_WHITELIST_SUBMISSION_TIMES.get(discordUserId);
        if (lastSubmission == null) {
            return false;
        }

        long cooldownMillis = cooldownSeconds * 1000L;
        return System.currentTimeMillis() - lastSubmission < cooldownMillis;
    }

    private void markWhitelistSubmitted(String discordUserId) {
        LAST_WHITELIST_SUBMISSION_TIMES.put(discordUserId, System.currentTimeMillis());
    }

    private boolean isApplicationExpired(WhitelistApplication application) {
        if (application == null) {
            return false;
        }

        int expiryHours = config.getApplicationExpiryHours();
        if (expiryHours <= 0) {
            return false;
        }

        long baseTime = application.getSubmittedAt();
        if (application.getReviewedAt() != null && application.getReviewedAt() > 0L) {
            baseTime = application.getReviewedAt();
        }

        long expiryMillis = expiryHours * 60L * 60L * 1000L;
        return System.currentTimeMillis() - baseTime >= expiryMillis;
    }

    private boolean isRetryExpired(WhitelistApplication application) {
        if (application == null) {
            return false;
        }

        if (application.getStatus() != ApplicationStatus.APPROVED_PENDING_NAME_FIX) {
            return false;
        }

        int expiryHours = config.getRetryNameExpiryHours();
        if (expiryHours <= 0) {
            return false;
        }

        long baseTime = application.getReviewedAt() != null
                ? application.getReviewedAt()
                : application.getSubmittedAt();

        long expiryMillis = expiryHours * 60L * 60L * 1000L;
        return System.currentTimeMillis() - baseTime >= expiryMillis;
    }
}