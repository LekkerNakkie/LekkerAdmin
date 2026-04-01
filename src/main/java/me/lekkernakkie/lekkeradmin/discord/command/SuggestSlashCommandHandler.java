package me.lekkernakkie.lekkeradmin.discord.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.discord.embed.SuggestionEmbedFactory;
import me.lekkernakkie.lekkeradmin.discord.model.SuggestionStatus;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.service.link.LinkService;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SuggestSlashCommandHandler {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;
    private final SuggestionEmbedFactory embedFactory;
    private final LinkService linkService;

    public SuggestSlashCommandHandler(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.embedFactory = new SuggestionEmbedFactory(config);
        this.linkService = new LinkService(plugin);
    }

    public void handle(@NotNull SlashCommandInteractionEvent event) {
        if (!config.isSuggestionsEnabled()) {
            event.reply(config.getSuggestionDisabledMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String suggestion = event.getOption("suggestie") == null
                ? null
                : event.getOption("suggestie").getAsString();

        if (suggestion == null || suggestion.isBlank()) {
            event.reply(config.getSuggestionInvalidLengthMessage()
                            .replace("{min}", String.valueOf(config.getSuggestionMinLength()))
                            .replace("{max}", String.valueOf(config.getSuggestionMaxLength())))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        int minLength = config.getSuggestionMinLength();
        int maxLength = config.getSuggestionMaxLength();

        if (suggestion.length() < minLength || suggestion.length() > maxLength) {
            event.reply(config.getSuggestionInvalidLengthMessage()
                            .replace("{min}", String.valueOf(minLength))
                            .replace("{max}", String.valueOf(maxLength)))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final TextChannel suggestionChannel = event.getJDA().getTextChannelById(config.getSuggestionsChannelId());
        if (suggestionChannel == null) {
            event.reply(config.getSuggestionMissingChannelMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final TextChannel logChannel;
        if (config.isSuggestionReviewEnabled()
                && config.getSuggestionsLogChannelId() != null
                && !config.getSuggestionsLogChannelId().isBlank()) {
            logChannel = event.getJDA().getTextChannelById(config.getSuggestionsLogChannelId());
        } else {
            logChannel = null;
        }

        final String submitterDisplay = resolveDiscordName(event);
        final String minecraftName = resolveMinecraftName(event);
        final String rawSuggestion = suggestion.trim();

        event.deferReply(true).queue(hook ->
                suggestionChannel.sendMessageEmbeds(
                        embedFactory.createPublicEmbed(
                                "pending",
                                submitterDisplay,
                                minecraftName,
                                rawSuggestion,
                                SuggestionStatus.PENDING,
                                null
                        )
                ).queue(publicMessage -> {
                            publicMessage.editMessageEmbeds(
                                    embedFactory.createPublicEmbed(
                                            publicMessage.getId(),
                                            submitterDisplay,
                                            minecraftName,
                                            rawSuggestion,
                                            SuggestionStatus.PENDING,
                                            null
                                    )
                            ).queue();

                            publicMessage.addReaction(Emoji.fromUnicode(config.getSuggestionUpvoteEmoji())).queue();
                            publicMessage.addReaction(Emoji.fromUnicode(config.getSuggestionDownvoteEmoji())).queue();

                            createSuggestionThreadIfEnabled(publicMessage, rawSuggestion);

                            if (logChannel != null) {
                                logChannel.sendMessageEmbeds(
                                        embedFactory.createLogEmbed(
                                                publicMessage.getId(),
                                                submitterDisplay,
                                                minecraftName,
                                                rawSuggestion,
                                                SuggestionStatus.PENDING,
                                                null,
                                                0,
                                                0
                                        )
                                ).queue(logMessage -> {
                                    logMessage.addReaction(Emoji.fromUnicode(config.getSuggestionApproveEmoji())).queue();
                                    logMessage.addReaction(Emoji.fromUnicode(config.getSuggestionDenyEmoji())).queue();
                                });
                            }

                            hook.editOriginal(config.getSuggestionSubmittedMessage()).queue();
                        }, error ->
                                hook.editOriginal(config.getSuggestionMissingChannelMessage()).queue()
                )
        );
    }

    private void createSuggestionThreadIfEnabled(Message message, String suggestion) {
        String threadName = buildSuggestionThreadName(message, suggestion);

        message.createThreadChannel(threadName).queue(
                success -> {},
                error -> plugin.getLogger().warning("Failed to create suggestion thread: " + error.getMessage())
        );
    }

    private String buildSuggestionThreadName(Message message, String suggestion) {
        String id = message.getId();
        String cleanSuggestion = suggestion == null ? "Suggestie" : suggestion.trim();

        if (cleanSuggestion.length() > 70) {
            cleanSuggestion = cleanSuggestion.substring(0, 70).trim() + "...";
        }

        String threadName = "Suggestie • " + id;
        if (!cleanSuggestion.isBlank()) {
            threadName += " • " + cleanSuggestion;
        }

        if (threadName.length() > 100) {
            threadName = threadName.substring(0, 100);
        }

        return threadName;
    }

    private String resolveDiscordName(SlashCommandInteractionEvent event) {
        String globalName = event.getUser().getGlobalName();
        if (globalName != null && !globalName.isBlank()) {
            return globalName;
        }

        return event.getUser().getName();
    }

    private String resolveMinecraftName(SlashCommandInteractionEvent event) {
        Optional<DiscordMinecraftLink> link = linkService.findByDiscordUserId(event.getUser().getId());
        if (link.isPresent()) {
            String minecraftName = link.get().getMinecraftName();
            if (minecraftName != null && !minecraftName.isBlank()) {
                return minecraftName;
            }
        }

        return null;
    }
}