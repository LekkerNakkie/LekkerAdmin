package me.lekkernakkie.lekkeradmin.discord.interaction.button;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class ConfirmButtonHandler {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;

    public ConfirmButtonHandler(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public void handle(ButtonInteractionEvent event) {
        plugin.getLogger().info("ConfirmButtonHandler received button: " + event.getComponentId());
        event.reply(config.getDmApplicationCancelled())
                .setEphemeral(true)
                .queue();
    }
}