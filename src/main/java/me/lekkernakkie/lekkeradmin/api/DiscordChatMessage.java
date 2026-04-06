package me.lekkernakkie.lekkeradmin.api;

public record DiscordChatMessage(
        String guildId,
        String channelId,
        String authorId,
        String authorName,
        String content,
        boolean bot,
        boolean webhook
) {}