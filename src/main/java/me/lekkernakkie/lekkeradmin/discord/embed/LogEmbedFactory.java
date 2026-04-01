package me.lekkernakkie.lekkeradmin.discord.embed;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;

public class LogEmbedFactory {

    private final DCBotConfig config;

    public LogEmbedFactory(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public MessageEmbed createLogEmbed(String title, String message) {

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(title);
        embed.setDescription(message);
        embed.setColor(new Color(config.getLogEmbedColor()));

        return embed.build();
    }
}