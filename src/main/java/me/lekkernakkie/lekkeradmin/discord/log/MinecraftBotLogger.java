package me.lekkernakkie.lekkeradmin.discord.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogChannelConfig;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MinecraftBotLogger {

    private final LekkerAdmin plugin;

    public MinecraftBotLogger(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void send(LogTypeSettings settings, MinecraftLogMessage message) {
        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) {
            plugin.debug("MinecraftBotLogger skipped: JDA not available.");
            return;
        }

        if (settings == null || message == null) {
            return;
        }

        LogChannelConfig channelConfig = settings.getChannelConfig();
        if (channelConfig == null || channelConfig.getChannelId().isBlank()) {
            plugin.getLogger().warning("Minecraft bot log channel id is empty.");
            return;
        }

        JDA jda = plugin.getDiscordManager().getJda();
        TextChannel channel = jda.getTextChannelById(channelConfig.getChannelId());

        if (channel == null) {
            plugin.getLogger().warning("Could not find Discord text channel for Minecraft logs: " + channelConfig.getChannelId());
            return;
        }

        if (message.hasEmbed()) {
            if (message.hasContent()) {
                channel.sendMessage(message.getContent())
                        .addEmbeds(message.getEmbed())
                        .queue(
                                success -> {},
                                error -> plugin.getLogger().warning("Failed to send Minecraft bot embed log: " + error.getMessage())
                        );
            } else {
                channel.sendMessageEmbeds(message.getEmbed())
                        .queue(
                                success -> {},
                                error -> plugin.getLogger().warning("Failed to send Minecraft bot embed log: " + error.getMessage())
                        );
            }
        } else if (message.hasContent()) {
            channel.sendMessage(message.getContent())
                    .queue(
                            success -> {},
                            error -> plugin.getLogger().warning("Failed to send Minecraft bot text log: " + error.getMessage())
                    );
        }

        if (message.hasExtraLines()) {
            for (String line : message.getExtraLines()) {
                if (line == null || line.isBlank()) {
                    continue;
                }

                channel.sendMessage(line)
                        .queue(
                                success -> {},
                                error -> plugin.getLogger().warning("Failed to send Minecraft bot extra log line: " + error.getMessage())
                        );
            }
        }
    }
}