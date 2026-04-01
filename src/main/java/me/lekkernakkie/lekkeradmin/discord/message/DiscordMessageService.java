package me.lekkernakkie.lekkeradmin.discord.message;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.discord.DiscordManager;
import net.dv8tion.jda.api.JDA;

public class DiscordMessageService {

    protected final LekkerAdmin plugin;

    public DiscordMessageService(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    protected JDA getJda() {
        DiscordManager manager = plugin.getDiscordManager();
        return manager == null ? null : manager.getJda();
    }
}