package me.lekkernakkie.lekkeradmin.discord.message;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.discord.embed.PlayerApplicationEmbedFactory;
import me.lekkernakkie.lekkeradmin.discord.util.DiscordButtonFactory;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.awt.Color;
import java.util.function.Consumer;

public class DiscordDmService extends DiscordMessageService {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;
    private final PlayerApplicationEmbedFactory playerEmbedFactory;

    public DiscordDmService(LekkerAdmin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.playerEmbedFactory = new PlayerApplicationEmbedFactory(plugin);
    }

    public void sendWhitelistStartDm(User user) {
        sendWhitelistStartDm(user, success -> {}, error -> {});
    }

    public void sendWhitelistStartDm(User user, Consumer<Boolean> onSuccess, Consumer<Throwable> onFailure) {
        if (getJda() == null || user == null) {
            onFailure.accept(new IllegalStateException("JDA of user is null."));
            return;
        }

        MessageEmbed embed = new EmbedBuilder()
                .setTitle(config.getEmbedWhitelistStartTitle())
                .setDescription(config.getEmbedWhitelistStartDescription())
                .setColor(new Color(config.getReviewEmbedColor()))
                .setFooter(emptyToNull(config.getEmbedWhitelistStartFooter()))
                .build();

        user.openPrivateChannel().queue(channel ->
                        channel.sendMessageEmbeds(embed)
                                .addActionRow(DiscordButtonFactory.create(
                                        config.getWhitelistStartButtonStyle(),
                                        "whitelist:start",
                                        config.getWhitelistOpenButtonText()
                                ))
                                .queue(
                                        success -> onSuccess.accept(true),
                                        error -> {
                                            plugin.getLogger().warning("Failed to send whitelist start DM to " + user.getAsTag() + ": " + error.getMessage());
                                            onFailure.accept(error);
                                        }
                                ),
                error -> {
                    plugin.getLogger().warning("Failed to open DM channel for " + user.getAsTag() + ": " + error.getMessage());
                    onFailure.accept(error);
                }
        );
    }

    public void sendSubmittedOverviewDm(String discordUserId, WhitelistApplication application) {
        sendEmbedDm(discordUserId, playerEmbedFactory.createSubmittedEmbed(application));
    }

    public void sendApprovedOverviewDm(String discordUserId, WhitelistApplication application, String reason) {
        sendEmbedDm(discordUserId, playerEmbedFactory.createApprovedEmbed(application, reason));
    }

    public void sendDeniedOverviewDm(String discordUserId, WhitelistApplication application, String reason) {
        sendEmbedDm(discordUserId, playerEmbedFactory.createDeniedEmbed(application, reason));
    }

    public void sendInvalidMinecraftNameDm(String discordUserId, String applicationId, String attemptedName, String message) {
        if (getJda() == null || discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        MessageEmbed embed = new EmbedBuilder()
                .setTitle(config.getEmbedInvalidNameTitle())
                .setDescription(message == null || message.isBlank() ? config.getEmbedInvalidNameDescription() : message)
                .addField(config.getEmbedMinecraftNameFieldName(), attemptedName == null || attemptedName.isBlank() ? "-" : attemptedName, false)
                .setColor(new Color(config.getDeniedEmbedColor()))
                .setFooter(emptyToNull(config.getEmbedInvalidNameFooter()))
                .build();

        getJda().retrieveUserById(discordUserId).queue(user ->
                        user.openPrivateChannel().queue(channel ->
                                        channel.sendMessageEmbeds(embed)
                                                .addActionRow(DiscordButtonFactory.create(
                                                        config.getRetryNameButtonStyle(),
                                                        "whitelist:retryname:" + applicationId,
                                                        config.getRetryNameButtonText()
                                                ))
                                                .queue(
                                                        success -> {},
                                                        error -> {
                                                            plugin.getLogger().warning("Failed to send invalid-name DM to " + discordUserId + ": " + error.getMessage());
                                                            error.printStackTrace();
                                                        }
                                                ),
                                error -> {
                                    plugin.getLogger().warning("Failed to open DM channel for invalid-name message to " + discordUserId + ": " + error.getMessage());
                                    error.printStackTrace();
                                }),
                error -> {
                    plugin.getLogger().warning("Failed to retrieve Discord user for invalid-name DM: " + discordUserId + ": " + error.getMessage());
                    error.printStackTrace();
                });
    }

    public void sendEmbedDm(String discordUserId, MessageEmbed embed) {
        if (getJda() == null || discordUserId == null || discordUserId.isBlank()) {
            return;
        }

        getJda().retrieveUserById(discordUserId).queue(user ->
                        user.openPrivateChannel().queue(channel ->
                                        channel.sendMessageEmbeds(embed).queue(
                                                success -> {},
                                                error -> {
                                                    plugin.getLogger().warning("Failed to send embed DM to " + discordUserId + ": " + error.getMessage());
                                                    error.printStackTrace();
                                                }
                                        ),
                                error -> {
                                    plugin.getLogger().warning("Failed to open DM channel for embed DM to " + discordUserId + ": " + error.getMessage());
                                    error.printStackTrace();
                                }),
                error -> {
                    plugin.getLogger().warning("Failed to retrieve Discord user for embed DM: " + discordUserId + ": " + error.getMessage());
                    error.printStackTrace();
                });
    }

    private String emptyToNull(String input) {
        return input == null || input.isBlank() ? null : input;
    }
}