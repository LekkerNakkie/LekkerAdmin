package me.lekkernakkie.lekkeradmin.discord.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.config.PunishmentsConfig;
import me.lekkernakkie.lekkeradmin.discord.embed.PlayerInfoEmbedFactory;
import me.lekkernakkie.lekkeradmin.discord.user.DiscordPlayerInfoResolver;
import me.lekkernakkie.lekkeradmin.discord.user.DiscordRoleService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class PlayerInfoSlashCommandHandler {

    private final PunishmentsConfig config;
    private final DCBotConfig dcBotConfig;
    private final DiscordRoleService discordRoleService;
    private final DiscordPlayerInfoResolver resolver;
    private final PlayerInfoEmbedFactory embedFactory;

    public PlayerInfoSlashCommandHandler(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getPunishmentsConfig();
        this.dcBotConfig = plugin.getConfigManager().getDcBotConfig();
        this.discordRoleService = new DiscordRoleService(plugin);
        this.resolver = new DiscordPlayerInfoResolver(plugin);
        this.embedFactory = new PlayerInfoEmbedFactory(config);
    }

    public void handle(@NotNull SlashCommandInteractionEvent event) {
        if (!config.isPlayerInfoEnabled()) {
            event.reply("Playerinfo is uitgeschakeld.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!hasPermission(event)) {
            event.reply("Je hebt geen permissie om dit commando te gebruiken.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String spelernaam = event.getOption("spelernaam") == null
                ? null
                : event.getOption("spelernaam").getAsString();

        if (spelernaam == null || spelernaam.isBlank()) {
            event.reply(config.getPlayerInfoNoResultsMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Optional<DiscordPlayerInfoResolver.PlayerInfoResult> result = resolver.resolve(
                spelernaam,
                config.getPlayerInfoActiveLimit(),
                config.getPlayerInfoExpiredLimit()
        );

        if (result.isEmpty()) {
            event.reply(config.getPlayerInfoNoResultsMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.replyEmbeds(embedFactory.create(result.get()))
                .setEphemeral(true)
                .queue();
    }

    private boolean hasPermission(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return false;
        }

        String userId = event.getUser().getId();
        List<String> allowedUserIds = dcBotConfig.getPlayerInfoAllowedUserIds();
        if (allowedUserIds != null && allowedUserIds.contains(userId)) {
            return true;
        }

        List<String> allowedRoleIds = dcBotConfig.getPlayerInfoAllowedRoleIds();
        if (allowedRoleIds != null && !allowedRoleIds.isEmpty()) {
            return member.getRoles().stream()
                    .anyMatch(role -> allowedRoleIds.contains(role.getId()));
        }

        return false;
    }
}