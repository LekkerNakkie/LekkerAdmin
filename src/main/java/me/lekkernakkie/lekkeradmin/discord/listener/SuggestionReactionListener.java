package me.lekkernakkie.lekkeradmin.discord.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.discord.embed.SuggestionEmbedFactory;
import me.lekkernakkie.lekkeradmin.discord.model.SuggestionStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SuggestionReactionListener extends ListenerAdapter {

    private final DCBotConfig config;
    private final SuggestionEmbedFactory embedFactory;

    public SuggestionReactionListener(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.embedFactory = new SuggestionEmbedFactory(config);
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (!config.isSuggestionsEnabled()) {
            return;
        }

        if (event.getUser() == null || event.getUser().isBot()) {
            return;
        }

        if (event.getGuild() == null) {
            return;
        }

        String channelId = event.getChannel().getId();
        String emoji = event.getReaction().getEmoji().getName();

        if (channelId.equals(config.getSuggestionsChannelId())) {
            handleVoteReaction(event, emoji);
            return;
        }

        if (config.isSuggestionReviewEnabled()
                && config.getSuggestionsLogChannelId() != null
                && channelId.equals(config.getSuggestionsLogChannelId())) {
            handleStaffReaction(event, emoji);
        }
    }

    private void handleVoteReaction(MessageReactionAddEvent event, String emoji) {
        String upvote = config.getSuggestionUpvoteEmoji();
        String downvote = config.getSuggestionDownvoteEmoji();

        if (!emoji.equals(upvote) && !emoji.equals(downvote)) {
            return;
        }

        event.retrieveMessage().queue(message -> {
            if (!isBotSuggestionMessage(message.getEmbeds())) {
                return;
            }

            if (config.isSuggestionPreventDoubleVote()) {
                String opposite = emoji.equals(upvote) ? downvote : upvote;
                message.removeReaction(Emoji.fromUnicode(opposite), event.getUser()).queue(
                        success -> updateLogVotesForPublicMessage(message.getId()),
                        error -> updateLogVotesForPublicMessage(message.getId())
                );
            } else {
                updateLogVotesForPublicMessage(message.getId());
            }
        });
    }

    private void handleStaffReaction(MessageReactionAddEvent event, String emoji) {
        String approve = config.getSuggestionApproveEmoji();
        String deny = config.getSuggestionDenyEmoji();

        if (!emoji.equals(approve) && !emoji.equals(deny)) {
            return;
        }

        Member member = event.getMember();
        if (!hasSuggestionReviewPermission(member)) {
            event.retrieveMessage().queue(message ->
                    message.removeReaction(Emoji.fromUnicode(emoji), event.getUser()).queue(
                            success -> {},
                            error -> {}
                    )
            );
            return;
        }

        event.retrieveMessage().queue(logMessage -> {
            if (!isBotSuggestionMessage(logMessage.getEmbeds())) {
                return;
            }

            if (logMessage.getEmbeds().isEmpty() || logMessage.getEmbeds().get(0).getFooter() == null) {
                return;
            }

            String footer = logMessage.getEmbeds().get(0).getFooter().getText();
            if (footer == null || !footer.startsWith(SuggestionEmbedFactory.LOG_PUBLIC_ID_PREFIX)) {
                return;
            }

            String publicMessageId = footer.substring(SuggestionEmbedFactory.LOG_PUBLIC_ID_PREFIX.length()).trim();
            SuggestionStatus newStatus = emoji.equals(approve) ? SuggestionStatus.APPROVED : SuggestionStatus.DENIED;
            String reviewer = resolveDiscordName(event);

            updateSuggestionEmbed(logMessage.getChannel().asTextChannel(), logMessage.getId(), newStatus, reviewer);

            TextChannel publicChannel = event.getJDA().getTextChannelById(config.getSuggestionsChannelId());
            if (publicChannel != null) {
                updateSuggestionEmbed(publicChannel, publicMessageId, newStatus, reviewer);
            }

            String opposite = emoji.equals(approve) ? deny : approve;
            logMessage.removeReaction(Emoji.fromUnicode(opposite), event.getUser()).queue(
                    success -> {},
                    error -> {}
            );

            updateLogVotesForPublicMessage(publicMessageId);
        });
    }

    private void updateSuggestionEmbed(TextChannel channel, String messageId, SuggestionStatus status, String reviewer) {
        channel.retrieveMessageById(messageId).queue(message -> {
            if (message.getEmbeds().isEmpty()) {
                return;
            }

            MessageEmbed existing = message.getEmbeds().get(0);
            EmbedBuilder builder = new EmbedBuilder(existing);
            builder.setColor(getStatusColor(status));

            List<MessageEmbed.Field> updatedFields = new ArrayList<>();
            boolean statusFieldUpdated = false;
            boolean reviewerFieldUpdated = false;

            for (MessageEmbed.Field field : existing.getFields()) {
                if (field.getName().equalsIgnoreCase(config.getSuggestionFieldStatusName())) {
                    updatedFields.add(new MessageEmbed.Field(
                            config.getSuggestionFieldStatusName(),
                            getStatusText(status),
                            true,
                            false
                    ));
                    statusFieldUpdated = true;
                    continue;
                }

                if (field.getName().equalsIgnoreCase(config.getSuggestionFieldReviewerName())) {
                    updatedFields.add(new MessageEmbed.Field(
                            config.getSuggestionFieldReviewerName(),
                            reviewer,
                            true,
                            false
                    ));
                    reviewerFieldUpdated = true;
                    continue;
                }

                updatedFields.add(field);
            }

            if (!statusFieldUpdated) {
                updatedFields.add(new MessageEmbed.Field(
                        config.getSuggestionFieldStatusName(),
                        getStatusText(status),
                        true,
                        false
                ));
            }

            if (!reviewerFieldUpdated) {
                updatedFields.add(new MessageEmbed.Field(
                        config.getSuggestionFieldReviewerName(),
                        reviewer,
                        true,
                        false
                ));
            }

            builder.clearFields();
            for (MessageEmbed.Field field : updatedFields) {
                builder.addField(field.getName(), field.getValue(), field.isInline());
            }

            message.editMessageEmbeds(builder.build()).queue();
        }, error -> {});
    }

    private void updateLogVotesForPublicMessage(String publicMessageId) {
        TextChannel publicChannel = getJdaTextChannel(config.getSuggestionsChannelId());
        TextChannel logChannel = getJdaTextChannel(config.getSuggestionsLogChannelId());

        if (publicChannel == null || logChannel == null) {
            return;
        }

        publicChannel.retrieveMessageById(publicMessageId).queue(publicMessage -> {
            int upvotes = countVotes(publicMessage, config.getSuggestionUpvoteEmoji());
            int downvotes = countVotes(publicMessage, config.getSuggestionDownvoteEmoji());

            findLogMessageByPublicMessageId(logChannel, publicMessageId, logMessage -> {
                if (logMessage.getEmbeds().isEmpty()) {
                    return;
                }

                MessageEmbed existing = logMessage.getEmbeds().get(0);
                String submitter = getFieldValue(existing, config.getSuggestionFieldSubmitterName());
                String minecraftName = getFieldValue(existing, "Minecraft naam");
                String reviewer = getFieldValue(existing, config.getSuggestionFieldReviewerName());
                String suggestionText = existing.getDescription();
                SuggestionStatus status = getStatusFromEmbed(existing);

                logMessage.editMessageEmbeds(
                        embedFactory.createLogEmbed(
                                publicMessageId,
                                submitter,
                                minecraftName,
                                suggestionText == null ? "" : suggestionText,
                                status,
                                reviewer,
                                upvotes,
                                downvotes
                        )
                ).queue();
            });
        }, error -> {});
    }

    private void findLogMessageByPublicMessageId(TextChannel logChannel, String publicMessageId, Consumer<Message> consumer) {
        logChannel.getHistory().retrievePast(100).queue(messages -> {
            for (Message message : messages) {
                if (message.getEmbeds().isEmpty()) {
                    continue;
                }

                MessageEmbed embed = message.getEmbeds().get(0);
                if (embed.getFooter() == null || embed.getFooter().getText() == null) {
                    continue;
                }

                String footer = embed.getFooter().getText();
                if (footer.equals(SuggestionEmbedFactory.LOG_PUBLIC_ID_PREFIX + publicMessageId)) {
                    consumer.accept(message);
                    return;
                }
            }
        }, error -> {});
    }

    private int countVotes(Message message, String emojiName) {
        for (MessageReaction reaction : message.getReactions()) {
            if (!reaction.getEmoji().getName().equals(emojiName)) {
                continue;
            }

            int count = reaction.getCount();
            return Math.max(0, count - 1);
        }

        return 0;
    }

    private SuggestionStatus getStatusFromEmbed(MessageEmbed embed) {
        String statusText = getFieldValue(embed, config.getSuggestionFieldStatusName());

        if (statusText == null) {
            return SuggestionStatus.PENDING;
        }

        if (statusText.equalsIgnoreCase(config.getSuggestionStatusApprovedText())) {
            return SuggestionStatus.APPROVED;
        }

        if (statusText.equalsIgnoreCase(config.getSuggestionStatusDeniedText())) {
            return SuggestionStatus.DENIED;
        }

        return SuggestionStatus.PENDING;
    }

    private String getFieldValue(MessageEmbed embed, String fieldName) {
        if (embed == null || embed.getFields() == null) {
            return null;
        }

        for (MessageEmbed.Field field : embed.getFields()) {
            if (field.getName().equalsIgnoreCase(fieldName)) {
                return field.getValue();
            }
        }

        return null;
    }

    private TextChannel getJdaTextChannel(String channelId) {
        try {
            if (channelId == null || channelId.isBlank()) {
                return null;
            }

            LekkerAdmin plugin = LekkerAdmin.getInstance();
            if (plugin == null || plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) {
                return null;
            }

            return plugin.getDiscordManager().getJda().getTextChannelById(channelId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isBotSuggestionMessage(List<MessageEmbed> embeds) {
        if (embeds == null || embeds.isEmpty()) {
            return false;
        }

        MessageEmbed embed = embeds.get(0);
        return embed.getTitle() != null
                && (embed.getTitle().equalsIgnoreCase(config.getSuggestionEmbedTitle())
                || embed.getTitle().equalsIgnoreCase(config.getSuggestionLogEmbedTitle()));
    }

    private boolean hasSuggestionReviewPermission(Member member) {
        if (member == null) {
            return false;
        }

        List<String> allowedRoleIds = config.getSuggestionReviewAllowedRoleIds();
        if (allowedRoleIds == null || allowedRoleIds.isEmpty()) {
            return false;
        }

        return member.getRoles().stream().anyMatch(role -> allowedRoleIds.contains(role.getId()));
    }

    private int getStatusColor(SuggestionStatus status) {
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

    private String resolveDiscordName(MessageReactionAddEvent event) {
        if (event.getUser() == null) {
            return "Onbekend";
        }

        String globalName = event.getUser().getGlobalName();
        if (globalName != null && !globalName.isBlank()) {
            return globalName;
        }

        return event.getUser().getName();
    }
}