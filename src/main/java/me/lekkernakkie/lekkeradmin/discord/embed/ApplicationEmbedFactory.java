package me.lekkernakkie.lekkeradmin.discord.embed;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;

public class ApplicationEmbedFactory {

    private final DCBotConfig config;

    public ApplicationEmbedFactory(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public MessageEmbed createApplicationEmbed(WhitelistApplication application) {

        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("Whitelist Application");
        embed.setColor(new Color(config.getReviewEmbedColor()));

        embed.addField("Minecraft Name", application.getMinecraftName(), false);
        embed.addField("Discord User", "<@" + application.getDiscordUserId() + ">", false);

        embed.setFooter("Application ID: " + application.getApplicationId());

        return embed.build();
    }
}