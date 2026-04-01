package me.lekkernakkie.lekkeradmin.discord.embed;

import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.discord.model.SuggestionStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class SuggestionEmbedFactory {

    public static final String LOG_PUBLIC_ID_PREFIX = "PUBLIC_MESSAGE_ID:";

    private final DCBotConfig config;

    public SuggestionEmbedFactory(DCBotConfig config) {
        this.config = config;
    }

    public MessageEmbed createPublicEmbed(String publicMessageId,
                                          String submitterDisplay,
                                          String minecraftName,
                                          String suggestionText,
                                          SuggestionStatus status,
                                          String reviewerDisplay) {

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(config.getSuggestionEmbedTitle())
                .setDescription(suggestionText)
                .setColor(getColor(status))
                .addField(config.getSuggestionFieldSubmitterName(), submitterDisplay, false);

        if (minecraftName != null && !minecraftName.isBlank()) {
            builder.addField("Minecraft naam", minecraftName, false);
        }

        builder.addField(config.getSuggestionFieldStatusName(), getStatusText(status), true)
                .setFooter(buildPublicFooter(publicMessageId));

        if (reviewerDisplay != null && !reviewerDisplay.isBlank() && status != SuggestionStatus.PENDING) {
            builder.addField(config.getSuggestionFieldReviewerName(), reviewerDisplay, true);
        }

        return builder.build();
    }

    public MessageEmbed createLogEmbed(String publicMessageId,
                                       String submitterDisplay,
                                       String minecraftName,
                                       String suggestionText,
                                       SuggestionStatus status,
                                       String reviewerDisplay,
                                       int upvotes,
                                       int downvotes) {

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(config.getSuggestionLogEmbedTitle())
                .setDescription(suggestionText)
                .setColor(getColor(status))
                .addField(config.getSuggestionFieldSubmitterName(), submitterDisplay, false);

        if (minecraftName != null && !minecraftName.isBlank()) {
            builder.addField("Minecraft naam", minecraftName, false);
        }

        builder.addField(config.getSuggestionFieldStatusName(), getStatusText(status), true);

        if (reviewerDisplay != null && !reviewerDisplay.isBlank() && status != SuggestionStatus.PENDING) {
            builder.addField(config.getSuggestionFieldReviewerName(), reviewerDisplay, true);
        }

        builder.addField("Upvotes", String.valueOf(upvotes), true);
        builder.addField("Downvotes", String.valueOf(downvotes), true);
        builder.setFooter(buildLogFooter(publicMessageId));

        return builder.build();
    }

    private int getColor(SuggestionStatus status) {
        return switch (status) {
            case APPROVED -> config.getSuggestionApprovedColor();
            case DENIED -> config.getSuggestionDeniedColor();
            case PENDING -> config.getSuggestionPendingColor();
        };
    }

    private String getStatusText(SuggestionStatus status) {
        return switch (status) {
            case APPROVED -> config.getSuggestionStatusApprovedText();
            case DENIED -> config.getSuggestionStatusDeniedText();
            case PENDING -> config.getSuggestionStatusPendingText();
        };
    }

    private String buildPublicFooter(String publicMessageId) {
        String shortId = toShortId(publicMessageId);
        String footer = config.getSuggestionEmbedFooter();

        if (footer == null || footer.isBlank()) {
            return shortId;
        }

        return footer + " • " + shortId;
    }

    private String buildLogFooter(String publicMessageId) {
        return LOG_PUBLIC_ID_PREFIX + publicMessageId;
    }

    private String toShortId(String messageId) {
        if (messageId == null || messageId.isBlank() || messageId.equalsIgnoreCase("pending")) {
            return "#PENDING";
        }

        int len = Math.min(6, messageId.length());
        return "#" + messageId.substring(messageId.length() - len);
    }
}