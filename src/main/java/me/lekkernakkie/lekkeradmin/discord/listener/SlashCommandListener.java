package me.lekkernakkie.lekkeradmin.discord.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.discord.command.PlayerInfoSlashCommandHandler;
import me.lekkernakkie.lekkeradmin.discord.command.SuggestSlashCommandHandler;
import me.lekkernakkie.lekkeradmin.discord.message.DiscordDmService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;

public class SlashCommandListener extends ListenerAdapter {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;
    private final DiscordDmService discordDmService;
    private final PlayerInfoSlashCommandHandler playerInfoHandler;
    private final SuggestSlashCommandHandler suggestHandler;

    public SlashCommandListener(LekkerAdmin plugin) {
        this.plugin = plugin;
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

        if (event.getName().equalsIgnoreCase("playerlist")) {
            handlePlayerListCommand(event);
            return;
        }

        if (event.getName().equalsIgnoreCase("status")) {
            handleStatusCommand(event);
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

    // Laat je bestaande playerlist handler gewoon staan als je die al hebt.
    private void handlePlayerListCommand(SlashCommandInteractionEvent event) {
        event.reply("Playerlist handler is already present in your codebase.")
                .setEphemeral(true)
                .queue();
    }

    private void handleStatusCommand(SlashCommandInteractionEvent event) {
        if (!config.isStatusCommandEnabled()) {
            event.reply(config.getStatusCommandDisabledMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(config.isStatusCommandEphemeral()).queue(hook ->
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    ServiceCheck sessionServer = checkUrl("https://sessionserver.mojang.com/");
                    ServiceCheck minecraftServices = checkUrl("https://api.minecraftservices.com/publickeys");
                    ServiceCheck mojangApi = checkUrl("https://api.mojang.com/");
                    ServiceCheck textures = checkUrl("https://textures.minecraft.net/");

                    int onlinePlayers = Bukkit.getOnlinePlayers().size();
                    int maxPlayers = Bukkit.getMaxPlayers();

                    String serverStatus = "🟢 Online";
                    String discordStatus = "🟢 Verbonden";

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(config.getStatusCommandEmbedTitle());
                    embed.setDescription(config.getStatusCommandEmbedDescription());
                    embed.setColor(parseColor(config.getStatusCommandEmbedColor(), "#30F2FF"));
                    embed.setTimestamp(Instant.now());

                    if (!config.getStatusCommandEmbedFooter().isBlank()) {
                        embed.setFooter(config.getStatusCommandEmbedFooter());
                    }

                    embed.addField("Server Status", serverStatus, true);
                    embed.addField("Players", onlinePlayers + "/" + maxPlayers, true);
                    embed.addField("Discord Bot", discordStatus, true);

                    embed.addField("Session Server", formatCheck(sessionServer), false);
                    embed.addField("Minecraft Services", formatCheck(minecraftServices), false);
                    embed.addField("Mojang API", formatCheck(mojangApi), false);
                    embed.addField("Textures CDN", formatCheck(textures), false);

                    hook.editOriginalEmbeds(embed.build()).queue();
                })
        );
    }

    private ServiceCheck checkUrl(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(config.getStatusCommandTimeoutMillis());
            connection.setReadTimeout(config.getStatusCommandTimeoutMillis());
            connection.setInstanceFollowRedirects(true);

            int code = connection.getResponseCode();
            boolean ok = code >= 200 && code < 500;

            return new ServiceCheck(ok, code, url);
        } catch (Exception ex) {
            return new ServiceCheck(false, -1, url);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String formatCheck(ServiceCheck check) {
        if (check.ok()) {
            return "🟢 Bereikbaar (`HTTP " + check.httpCode() + "`)";
        }

        if (check.httpCode() > 0) {
            return "🟡 Error (`HTTP " + check.httpCode() + "`)";
        }

        return "🔴 Onbereikbaar";
    }

    private Color parseColor(String input, String fallback) {
        try {
            return Color.decode(input);
        } catch (Exception ex) {
            try {
                return Color.decode(fallback);
            } catch (Exception ignored) {
                return new Color(0x30F2FF);
            }
        }
    }

    private record ServiceCheck(boolean ok, int httpCode, String url) {
    }
}