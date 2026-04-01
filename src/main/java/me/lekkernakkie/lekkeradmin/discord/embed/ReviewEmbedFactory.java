package me.lekkernakkie.lekkeradmin.discord.embed;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.model.application.ApplicationAnswer;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

public class ReviewEmbedFactory {

    private final DCBotConfig config;

    public ReviewEmbedFactory(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public MessageEmbed createReviewEmbed(WhitelistApplication application) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(config.getEmbedReviewTitle());
        if (config.getEmbedReviewDescription() != null && !config.getEmbedReviewDescription().isBlank()) {
            embed.setDescription(config.getEmbedReviewDescription());
        }
        embed.setColor(new Color(config.getReviewEmbedColor()));

        addBaseApplicationFields(embed, application);
        addAnswerFields(embed, application.getAnswers());

        String footer = config.getEmbedReviewFooter();
        if (footer != null && !footer.isBlank()) {
            embed.setFooter(footer + " | Application ID: " + safe(application.getApplicationId()));
        } else {
            embed.setFooter("Application ID: " + safe(application.getApplicationId()));
        }

        return embed.build();
    }

    public MessageEmbed createApprovedEmbed(
            WhitelistApplication application,
            String reviewer,
            String reason
    ) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(config.getStaffApproved());
        if (config.getEmbedApprovedDescription() != null && !config.getEmbedApprovedDescription().isBlank()) {
            embed.setDescription(config.getEmbedApprovedDescription());
        }
        embed.setColor(new Color(config.getApprovedEmbedColor()));

        addBaseApplicationFields(embed, application);
        addAnswerFields(embed, application.getAnswers());

        embed.addField(config.getEmbedReviewerFieldName(), safe(reviewer), false);

        if (reason != null && !reason.isBlank()) {
            embed.addField(config.getEmbedApprovedReasonFieldName(), reason, false);
        }

        String footer = config.getEmbedApprovedFooter();
        if (footer != null && !footer.isBlank()) {
            embed.setFooter(footer + " | Application ID: " + safe(application.getApplicationId()));
        } else {
            embed.setFooter("Application ID: " + safe(application.getApplicationId()));
        }

        return embed.build();
    }

    public MessageEmbed createDeniedEmbed(
            WhitelistApplication application,
            String reviewer,
            String reason
    ) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(config.getStaffDenied());
        if (config.getEmbedDeniedDescription() != null && !config.getEmbedDeniedDescription().isBlank()) {
            embed.setDescription(config.getEmbedDeniedDescription());
        }
        embed.setColor(new Color(config.getDeniedEmbedColor()));

        addBaseApplicationFields(embed, application);
        addAnswerFields(embed, application.getAnswers());

        embed.addField(config.getEmbedReviewerFieldName(), safe(reviewer), false);

        if (reason != null && !reason.isBlank()) {
            embed.addField(config.getEmbedApprovedReasonFieldName(), reason, false);
        }

        String footer = config.getEmbedDeniedFooter();
        if (footer != null && !footer.isBlank()) {
            embed.setFooter(footer + " | Application ID: " + safe(application.getApplicationId()));
        } else {
            embed.setFooter("Application ID: " + safe(application.getApplicationId()));
        }

        return embed.build();
    }

    public MessageEmbed createPendingFixEmbed(
            WhitelistApplication application,
            String reviewer,
            String message
    ) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(config.getStaffPendingNameFix());
        if (config.getEmbedInvalidNameDescription() != null && !config.getEmbedInvalidNameDescription().isBlank()) {
            embed.setDescription(config.getEmbedInvalidNameDescription());
        }
        embed.setColor(new Color(config.getPendingFixEmbedColor()));

        addBaseApplicationFields(embed, application);
        addAnswerFields(embed, application.getAnswers());

        embed.addField(config.getEmbedReviewerFieldName(), safe(reviewer), false);
        embed.addField(config.getEmbedProblemFieldName(), safe(message), false);

        String footer = config.getEmbedInvalidNameFooter();
        if (footer != null && !footer.isBlank()) {
            embed.setFooter(footer + " | Application ID: " + safe(application.getApplicationId()));
        } else {
            embed.setFooter("Application ID: " + safe(application.getApplicationId()));
        }

        return embed.build();
    }

    private void addBaseApplicationFields(EmbedBuilder embed, WhitelistApplication application) {
        embed.addField(config.getEmbedMinecraftNameFieldName(), safe(application.getMinecraftName()), false);
        embed.addField(
                config.getEmbedDiscordFieldName(),
                safe(application.getDiscordTag()) + " (<@" + safe(application.getDiscordUserId()) + ">)",
                false
        );
    }

    private void addAnswerFields(EmbedBuilder embed, List<ApplicationAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }

        String minecraftFieldName = normalize(config.getEmbedMinecraftNameFieldName());
        String discordFieldName = normalize(config.getEmbedDiscordFieldName());

        for (ApplicationAnswer answer : answers) {
            if (answer == null) {
                continue;
            }

            String fieldKey = normalize(answer.getFieldKey());
            String label = answer.getFieldLabel() == null || answer.getFieldLabel().isBlank()
                    ? answer.getFieldKey()
                    : answer.getFieldLabel();

            String normalizedLabel = normalize(label);

            if (fieldKey.equals("minecraft_name")
                    || normalizedLabel.equals(minecraftFieldName)
                    || fieldKey.equals("discord")
                    || normalizedLabel.equals(discordFieldName)) {
                continue;
            }

            String value = answer.getFieldValue();
            if (value == null || value.isBlank()) {
                value = "-";
            }

            if (value.length() > 1000) {
                value = value.substring(0, 997) + "...";
            }

            embed.addField(label, value, false);
        }
    }

    private String safe(String input) {
        return input == null || input.isBlank() ? "-" : input;
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase(Locale.ROOT);
    }
}