package me.lekkernakkie.lekkeradmin.discord.interaction.modal;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class NameRetryModalFactory {

    private final DCBotConfig config;

    public NameRetryModalFactory(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public Modal create(String applicationId) {
        TextInput input = TextInput.create(
                        "field:minecraft_name",
                        config.getNameRetryFieldLabel(),
                        parseStyle(config.getNameRetryFieldStyle())
                )
                .setPlaceholder(config.getNameRetryFieldPlaceholder())
                .setRequired(config.isNameRetryFieldRequired())
                .setRequiredRange(
                        config.getNameRetryFieldMinLength(),
                        config.getNameRetryFieldMaxLength()
                )
                .build();

        return Modal.create("whitelist:retryname:" + applicationId, config.getNameRetryModalTitle())
                .addActionRow(input)
                .build();
    }

    private TextInputStyle parseStyle(String style) {
        if (style == null) {
            return TextInputStyle.SHORT;
        }

        return switch (style.toUpperCase()) {
            case "PARAGRAPH" -> TextInputStyle.PARAGRAPH;
            default -> TextInputStyle.SHORT;
        };
    }
}