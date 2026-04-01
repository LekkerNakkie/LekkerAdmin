package me.lekkernakkie.lekkeradmin.discord;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.discord.listener.ButtonListener;
import me.lekkernakkie.lekkeradmin.discord.listener.ModalListener;
import me.lekkernakkie.lekkeradmin.discord.listener.SlashCommandListener;
import me.lekkernakkie.lekkeradmin.discord.listener.SuggestionReactionListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class DiscordRegistrar {

    private final LekkerAdmin plugin;
    private final JDA jda;

    public DiscordRegistrar(LekkerAdmin plugin, JDA jda) {
        this.plugin = plugin;
        this.jda = jda;
    }

    public void registerListeners() {
        jda.addEventListener(
                new SlashCommandListener(plugin),
                new ButtonListener(plugin),
                new ModalListener(plugin),
                new SuggestionReactionListener(plugin)
        );

        plugin.getLogger().info("Discord listeners registered.");
    }

    public void registerSlashCommands() {
        jda.updateCommands()
                .addCommands(
                        Commands.slash("whitelist", "Start je whitelist aanvraag"),
                        Commands.slash("playerinfo", "Bekijk informatie over een speler")
                                .addOption(OptionType.STRING, "spelernaam", "Minecraft naam, Discord naam of Discord ID", true),
                        Commands.slash("suggest", "Dien een suggestie in")
                                .addOption(OptionType.STRING, "suggestie", "Jouw suggestie", true)
                )
                .queue(
                        success -> plugin.getLogger().info("Discord slash commands registered."),
                        error -> {
                            plugin.getLogger().warning("Failed to register slash commands: " + error.getMessage());
                            error.printStackTrace();
                        }
                );
    }
}