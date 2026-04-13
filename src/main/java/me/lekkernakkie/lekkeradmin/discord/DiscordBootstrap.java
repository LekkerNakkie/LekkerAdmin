package me.lekkernakkie.lekkeradmin.discord;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBootstrap {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;

    public DiscordBootstrap(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public DiscordManager start() {
        if (!config.isBotEnabled()) {
            plugin.getLogger().info("Discord bot is disabled in DCBot.yml");
            return null;
        }

        if (config.getBotToken() == null
                || config.getBotToken().isBlank()
                || config.getBotToken().equals("PASTE_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("Discord bot is enabled, but no valid token is configured.");
            return null;
        }

        try {
            JDABuilder builder = JDABuilder.createDefault(config.getBotToken())
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    );

            JDA jda = builder.build();
            jda.awaitReady();

            /*      -- DUBBEL COMMAND VERWIJDEREN DISCORD BOT 
            if (config.getGuildId() != null && !config.getGuildId().isBlank()) {
                Guild guild = jda.getGuildById(config.getGuildId());
                if (guild != null) {
                    guild.updateCommands().complete();
                    plugin.getLogger().info("Cleared old guild slash commands.");
                } else {
                    plugin.getLogger().warning("Guild not found for command cleanup.");
                }
            }
            */

            DiscordManager manager = new DiscordManager(plugin, jda);
            manager.start();

            plugin.getLogger().info("Discord bot connected successfully.");
            return manager;

        } catch (Exception ex) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to start Discord bot.", ex);
            return null;
        }
    }
}