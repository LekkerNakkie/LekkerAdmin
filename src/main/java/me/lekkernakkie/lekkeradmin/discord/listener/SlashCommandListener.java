package me.lekkernakkie.lekkeradmin.discord.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.discord.command.PlayerInfoSlashCommandHandler;
import me.lekkernakkie.lekkeradmin.discord.command.SuggestSlashCommandHandler;
import me.lekkernakkie.lekkeradmin.discord.message.DiscordDmService;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SlashCommandListener extends ListenerAdapter {

    private final DCBotConfig config;
    private final DiscordDmService discordDmService;
    private final PlayerInfoSlashCommandHandler playerInfoHandler;
    private final SuggestSlashCommandHandler suggestHandler;

    public SlashCommandListener(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.discordDmService = new DiscordDmService(plugin);
        this.playerInfoHandler = new PlayerInfoSlashCommandHandler(plugin);
        this.suggestHandler = new SuggestSlashCommandHandler(plugin);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("whitelist")) {
            handleWhitelistCommand(event);
            return;
        }

        if (event.getName().equalsIgnoreCase("playerinfo")) {
            playerInfoHandler.handle(event);
            return;
        }

        if (event.getName().equalsIgnoreCase("suggest")) {
            suggestHandler.handle(event);
        }
    }

    private void handleWhitelistCommand(SlashCommandInteractionEvent event) {
        User user = event.getUser();

        event.deferReply(true).queue(hook ->
                discordDmService.sendWhitelistStartDm(
                        user,
                        success -> hook.editOriginal(config.getSlashWhitelistStarted()).queue(),
                        error -> hook.editOriginal(config.getSlashDmFailed()).queue()
                )
        );
    }
}