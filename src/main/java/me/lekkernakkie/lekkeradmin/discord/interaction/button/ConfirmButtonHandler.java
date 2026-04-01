package me.lekkernakkie.lekkeradmin.discord.interaction.button;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class ConfirmButtonHandler {

    private final LekkerAdmin plugin;

    public ConfirmButtonHandler(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void handle(ButtonInteractionEvent event) {
        plugin.getLogger().info("ConfirmButtonHandler received button: " + event.getComponentId());
        event.reply("Deze confirm flow wordt later uitgebreid.")
                .setEphemeral(true)
                .queue();
    }
}