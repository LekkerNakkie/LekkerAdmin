package me.lekkernakkie.lekkeradmin.discord.message;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.discord.embed.ReviewEmbedFactory;
import me.lekkernakkie.lekkeradmin.discord.util.DiscordButtonFactory;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordStaffReviewService {

    private static final Map<String, String> REVIEW_MESSAGE_IDS = new ConcurrentHashMap<>();

    private final LekkerAdmin plugin;
    private final DCBotConfig config;
    private final ReviewEmbedFactory embedFactory;

    public DiscordStaffReviewService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.embedFactory = new ReviewEmbedFactory(plugin);
    }

    public void postApplicationReview(WhitelistApplication application) {
        TextChannel channel = getReviewChannel();
        if (channel == null) {
            return;
        }

        channel.sendMessageEmbeds(embedFactory.createReviewEmbed(application))
                .setActionRow(
                        DiscordButtonFactory.create(
                                config.getApproveButtonStyle(),
                                "review:approve:" + application.getApplicationId(),
                                config.getApproveButtonText()
                        ),
                        DiscordButtonFactory.create(
                                config.getDenyButtonStyle(),
                                "review:deny:" + application.getApplicationId(),
                                config.getDenyButtonText()
                        ),
                        DiscordButtonFactory.create(
                                config.getApproveWithReasonButtonStyle(),
                                "review:approvereason:" + application.getApplicationId(),
                                config.getApproveWithReasonButtonText()
                        ),
                        DiscordButtonFactory.create(
                                config.getDenyWithReasonButtonStyle(),
                                "review:denyreason:" + application.getApplicationId(),
                                config.getDenyWithReasonButtonText()
                        )
                )
                .queue(
                        message -> REVIEW_MESSAGE_IDS.put(application.getApplicationId(), message.getId()),
                        error -> plugin.getLogger().warning("Failed to post review message for application "
                                + application.getApplicationId() + ": " + error.getMessage())
                );
    }

    public void editApprovedReview(WhitelistApplication application, String reviewer, String reason) {
        editReview(application, embedFactory.createApprovedEmbed(application, reviewer, reason));
    }

    public void editDeniedReview(WhitelistApplication application, String reviewer, String reason) {
        editReview(application, embedFactory.createDeniedEmbed(application, reviewer, reason));
    }

    public void editPendingNameFixReview(WhitelistApplication application, String reviewer, String message) {
        editReview(application, embedFactory.createPendingFixEmbed(application, reviewer, message));
    }

    private void editReview(WhitelistApplication application, MessageEmbed embed) {
        TextChannel channel = getReviewChannel();
        if (channel == null || application == null || application.getApplicationId() == null) {
            return;
        }

        String applicationId = application.getApplicationId();
        String cachedMessageId = REVIEW_MESSAGE_IDS.get(applicationId);

        if (cachedMessageId != null && !cachedMessageId.isBlank()) {
            channel.editMessageEmbedsById(cachedMessageId, embed)
                    .setComponents()
                    .queue(
                            success -> { },
                            error -> {
                                plugin.getLogger().warning("Failed to edit cached review message for application "
                                        + applicationId + ": " + error.getMessage() + ". Trying fallback lookup.");
                                REVIEW_MESSAGE_IDS.remove(applicationId);
                                findAndEditReviewMessage(channel, applicationId, embed);
                            }
                    );
            return;
        }

        findAndEditReviewMessage(channel, applicationId, embed);
    }

    private void findAndEditReviewMessage(TextChannel channel, String applicationId, MessageEmbed embed) {
        channel.getHistory().retrievePast(100).queue(
                messages -> {
                    Message found = findReviewMessage(messages, applicationId);
                    if (found == null) {
                        plugin.getLogger().warning("Could not find review message for application " + applicationId);
                        return;
                    }

                    REVIEW_MESSAGE_IDS.put(applicationId, found.getId());

                    channel.editMessageEmbedsById(found.getId(), embed)
                            .setComponents()
                            .queue(
                                    success -> { },
                                    error -> plugin.getLogger().warning("Failed to edit found review message for application "
                                            + applicationId + ": " + error.getMessage())
                            );
                },
                error -> plugin.getLogger().warning("Failed to fetch review history for application "
                        + applicationId + ": " + error.getMessage())
        );
    }

    private Message findReviewMessage(List<Message> messages, String applicationId) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        String needle = "Application ID: " + applicationId;

        for (Message message : messages) {
            if (message == null || message.getEmbeds().isEmpty()) {
                continue;
            }

            for (MessageEmbed embed : message.getEmbeds()) {
                if (embed == null) {
                    continue;
                }

                String footerText = embed.getFooter() != null ? embed.getFooter().getText() : null;
                if (footerText != null && footerText.contains(needle)) {
                    return message;
                }
            }
        }

        return null;
    }

    private TextChannel getReviewChannel() {
        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) {
            return null;
        }

        TextChannel channel = plugin.getDiscordManager()
                .getJda()
                .getTextChannelById(config.getReviewChannelId());

        if (channel == null) {
            plugin.getLogger().warning("Review channel not found.");
            return null;
        }

        return channel;
    }
}