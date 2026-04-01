package me.lekkernakkie.lekkeradmin.discord.interaction.button;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.discord.embed.ReviewEmbedFactory;
import me.lekkernakkie.lekkeradmin.discord.interaction.component.ComponentIdParser;
import me.lekkernakkie.lekkeradmin.discord.log.DiscordWebhookLogger;
import me.lekkernakkie.lekkeradmin.discord.message.DiscordDmService;
import me.lekkernakkie.lekkeradmin.discord.message.DiscordStaffReviewService;
import me.lekkernakkie.lekkeradmin.discord.user.DiscordRoleService;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import me.lekkernakkie.lekkeradmin.service.application.ApplicationService;
import me.lekkernakkie.lekkeradmin.service.whitelist.WhitelistService;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.Optional;

public class ReviewButtonHandler {

    private final ApplicationService applicationService;
    private final WhitelistService whitelistService;
    private final ReviewEmbedFactory embedFactory;
    private final DiscordWebhookLogger webhookLogger;
    private final DiscordDmService discordDmService;
    private final DiscordStaffReviewService discordStaffReviewService;
    private final DiscordRoleService discordRoleService;

    public ReviewButtonHandler(LekkerAdmin plugin) {
        this.applicationService = new ApplicationService(plugin);
        this.whitelistService = new WhitelistService(plugin);
        this.embedFactory = new ReviewEmbedFactory(plugin);
        this.webhookLogger = new DiscordWebhookLogger(plugin);
        this.discordDmService = new DiscordDmService(plugin);
        this.discordStaffReviewService = new DiscordStaffReviewService(plugin);
        this.discordRoleService = new DiscordRoleService(plugin);
    }

    public void handle(ButtonInteractionEvent event, ComponentIdParser.ParsedComponentId parsed) {
        if (parsed == null) {
            return;
        }

        String action = parsed.action();
        String applicationId = parsed.value();

        if (action.equalsIgnoreCase("approve")) {
            handleApprove(event, applicationId);
            return;
        }

        if (action.equalsIgnoreCase("deny")) {
            handleDeny(event, applicationId);
        }
    }

    private void handleApprove(ButtonInteractionEvent event, String applicationId) {
        Optional<WhitelistApplication> optional =
                applicationService.findByApplicationId(applicationId);

        if (optional.isEmpty()) {
            event.reply("Application not found").setEphemeral(true).queue();
            return;
        }

        WhitelistApplication application = optional.get();
        String reviewerId = event.getUser().getId();
        String reviewer = event.getUser().getAsTag();

        WhitelistService.WhitelistResult result =
                whitelistService.finalizeApprovedApplication(application, reviewerId, reviewer);

        if (!result.success()) {
            event.reply("Whitelist failed: " + result.message())
                    .setEphemeral(true)
                    .queue();

            if (result.needsNameFix()) {
                discordStaffReviewService.editPendingNameFixReview(
                        application,
                        reviewer,
                        result.message()
                );
            }

            return;
        }

        discordRoleService.giveApproveRole(application.getDiscordUserId());
        webhookLogger.logApproval(reviewer, application);

        discordDmService.sendApprovedOverviewDm(
                application.getDiscordUserId(),
                application,
                null
        );

        event.editMessageEmbeds(
                embedFactory.createApprovedEmbed(application, reviewer, null)
        ).setComponents().queue();

        discordStaffReviewService.editApprovedReview(application, reviewer, null);
    }

    private void handleDeny(ButtonInteractionEvent event, String applicationId) {
        Optional<WhitelistApplication> optional =
                applicationService.findByApplicationId(applicationId);

        if (optional.isEmpty()) {
            event.reply("Application not found").setEphemeral(true).queue();
            return;
        }

        WhitelistApplication application = optional.get();
        String reviewer = event.getUser().getAsTag();

        applicationService.markDenied(application, event.getUser().getId(), reviewer, null);
        webhookLogger.logDenial(reviewer, application, null);

        discordDmService.sendDeniedOverviewDm(
                application.getDiscordUserId(),
                application,
                null
        );

        event.editMessageEmbeds(
                embedFactory.createDeniedEmbed(application, reviewer, null)
        ).setComponents().queue();

        discordStaffReviewService.editDeniedReview(application, reviewer, null);
    }
}