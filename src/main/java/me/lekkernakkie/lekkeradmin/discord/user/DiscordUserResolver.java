package me.lekkernakkie.lekkeradmin.discord.user;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.discord.DiscordManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import java.util.Optional;

public class DiscordUserResolver {

    private final LekkerAdmin plugin;

    public DiscordUserResolver(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public Optional<User> findUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        DiscordManager manager = plugin.getDiscordManager();
        if (manager == null) {
            return Optional.empty();
        }

        JDA jda = manager.getJda();
        if (jda == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(jda.getUserById(userId));
    }
}