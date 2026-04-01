package me.lekkernakkie.lekkeradmin.discord;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import java.util.List;

public class DiscordManager {

    private final LekkerAdmin plugin;
    private final JDA jda;
    private final DCBotConfig config;
    private final DiscordRegistrar registrar;

    private DiscordStatusRotator statusRotator;

    public DiscordManager(LekkerAdmin plugin, JDA jda) {
        this.plugin = plugin;
        this.jda = jda;
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.registrar = new DiscordRegistrar(plugin, jda);
    }

    public void start() {
        applyPresence();
        registrar.registerListeners();
        registrar.registerSlashCommands();
    }

    public void shutdown() {
        if (statusRotator != null) {
            statusRotator.stop();
            statusRotator = null;
        }

        if (jda != null) {
            jda.shutdown();
            plugin.getLogger().info("Discord bot shut down.");
        }
    }

    private void applyPresence() {
        if (statusRotator != null) {
            statusRotator.stop();
            statusRotator = null;
        }

        jda.getPresence().setStatus(OnlineStatus.ONLINE);

        if (!config.isStatusEnabled()) {
            return;
        }

        List<String> messages = config.getStatusMessages();

        if (messages != null && !messages.isEmpty()) {
            this.statusRotator = new DiscordStatusRotator(
                    plugin,
                    jda,
                    messages,
                    config.getStatusType(),
                    config.getStatusIntervalSeconds()
            );
            this.statusRotator.start();
            plugin.debug("Discord rotating status started with " + messages.size() + " messages.");
            return;
        }

        String text = config.getStatusText();

        Activity activity = switch (config.getStatusType().toUpperCase()) {
            case "WATCHING" -> Activity.watching(text);
            case "LISTENING" -> Activity.listening(text);
            case "COMPETING" -> Activity.competing(text);
            default -> Activity.playing(text);
        };

        jda.getPresence().setPresence(OnlineStatus.ONLINE, activity);
        plugin.debug("Discord fallback static status applied: " + text);
    }

    public JDA getJda() {
        return jda;
    }

    public DiscordRegistrar getRegistrar() {
        return registrar;
    }
}