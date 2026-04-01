package me.lekkernakkie.lekkeradmin.discord.embed;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.model.application.ApplicationAnswer;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.util.List;

public class PlayerApplicationEmbedFactory {

    private final DCBotConfig config;

    public PlayerApplicationEmbedFactory(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public MessageEmbed createSubmittedEmbed(WhitelistApplication application) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(config.getEmbedSubmittedTitle());
        embed.setDescription(config.getEmbedSubmittedDescription());
        embed.setColor(new Color(config.getReviewEmbedColor()));

        addBaseFields(embed, application);
        addAnswerFields(embed, application.getAnswers());

        String footer = config.getEmbedSubmittedFooter();
        if (footer != null && !footer.isBlank()) {
            embed.setFooter(footer + " | Application ID: " + safe(application.getApplicationId()));
        } else {
            embed.setFooter("Application ID: " + safe(application.getApplicationId()));
        }

        return embed.build();
    }

    public MessageEmbed createApprovedEmbed(WhitelistApplication application, String reason) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(config.getEmbedApprovedTitle());
        embed.setDescription(config.getEmbedApprovedDescription());
        embed.setColor(new Color(config.getApprovedEmbedColor()));

        addBaseFields(embed, application);
        addAnswerFields(embed, application.getAnswers());

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

    public MessageEmbed createDeniedEmbed(WhitelistApplication application, String reason) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(config.getEmbedDeniedTitle());
        embed.setDescription(reason == null || reason.isBlank()
                ? config.getEmbedDeniedDescription()
                : config.getDmApplicationDeniedWithReason().replace("{reason}", reason));
        embed.setColor(new Color(config.getDeniedEmbedColor()));

        addBaseFields(embed, application);
        addAnswerFields(embed, application.getAnswers());

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

    public MessageEmbed createInvalidNameEmbed(WhitelistApplication application, String message) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(config.getEmbedInvalidNameTitle());
        embed.setDescription(message == null || message.isBlank()
                ? config.getEmbedInvalidNameDescription()
                : message);
        embed.setColor(new Color(config.getDeniedEmbedColor()));

        addBaseFields(embed, application);
        addAnswerFields(embed, application.getAnswers());

        String footer = config.getEmbedInvalidNameFooter();
        if (footer != null && !footer.isBlank()) {
            embed.setFooter(footer + " | Application ID: " + safe(application.getApplicationId()));
        } else {
            embed.setFooter("Application ID: " + safe(application.getApplicationId()));
        }

        return embed.build();
    }

    private void addBaseFields(EmbedBuilder embed, WhitelistApplication application) {
        embed.addField(config.getEmbedMinecraftNameFieldName(), safe(application.getMinecraftName()), false);
        embed.addField(config.getEmbedDiscordFieldName(), safe(application.getDiscordTag()), false);
    }

    private void addAnswerFields(EmbedBuilder embed, List<ApplicationAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }

        for (ApplicationAnswer answer : answers) {
            String label = answer.getFieldLabel() == null || answer.getFieldLabel().isBlank()
                    ? answer.getFieldKey()
                    : answer.getFieldLabel();

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
}