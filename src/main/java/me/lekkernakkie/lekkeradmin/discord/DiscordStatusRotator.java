package me.lekkernakkie.lekkeradmin.discord;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DiscordStatusRotator {

    private final LekkerAdmin plugin;
    private final JDA jda;
    private final List<String> messages;
    private final String type;
    private final int intervalSeconds;

    private final AtomicInteger index = new AtomicInteger(0);
    private BukkitTask task;

    public DiscordStatusRotator(LekkerAdmin plugin, JDA jda, List<String> messages, String type, int intervalSeconds) {
        this.plugin = plugin;
        this.jda = jda;
        this.messages = messages;
        this.type = type;
        this.intervalSeconds = Math.max(5, intervalSeconds);
    }

    public void start() {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        stop();

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                String raw = messages.get(index.getAndUpdate(current -> (current + 1) % messages.size()));
                String parsed = applyPlaceholders(raw);

                Activity activity = buildActivity(parsed);
                jda.getPresence().setActivity(activity);
            } catch (Exception ex) {
                plugin.debug("DiscordStatusRotator error: " + ex.getMessage());
            }
        }, 0L, intervalSeconds * 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private String applyPlaceholders(String input) {
        if (input == null) {
            return "";
        }

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();

        return input
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max));
    }

    private Activity buildActivity(String text) {
        return switch (type.toUpperCase()) {
            case "WATCHING" -> Activity.watching(text);
            case "LISTENING" -> Activity.listening(text);
            case "COMPETING" -> Activity.competing(text);
            default -> Activity.playing(text);
        };
    }
}