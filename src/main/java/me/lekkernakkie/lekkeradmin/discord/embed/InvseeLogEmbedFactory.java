package me.lekkernakkie.lekkeradmin.discord.embed;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogEmbedConfig;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftLogMessage;
import me.lekkernakkie.lekkeradmin.discord.util.DiscordColorUtil;
import me.lekkernakkie.lekkeradmin.model.log.InvseeLogContext;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;

public class InvseeLogEmbedFactory {

    private final LekkerAdmin plugin;

    public InvseeLogEmbedFactory(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public MinecraftLogMessage create(LogEmbedConfig config, InvseeLogContext context) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(config.getTitle());
        embed.setDescription("Invsee sessie afgerond.");
        embed.setColor(DiscordColorUtil.fromHex(config.getColor(), Color.decode("#5865F2")));
        embed.setTimestamp(Instant.now());

        if (config.getFooter() != null && !config.getFooter().isBlank()) {
            embed.setFooter(config.getFooter());
        }

        embed.addField("Staff", context.getStaffName(), true);
        embed.addField("Target", context.getTargetName(), true);
        embed.addField("Online", context.isTargetOnline() ? "Ja" : "Nee", true);
        embed.addField("Geopend om", context.getOpenedAt(), true);
        embed.addField("Gesloten om", context.getClosedAt(), true);
        embed.addField("Duur", context.getDuration(), true);
        embed.addField("Acties", context.getActionsSummary(20), false);

        return new MinecraftLogMessage("", embed.build(), new ArrayList<>());
    }
}