package me.lekkernakkie.lekkeradmin.discord.interaction.modal;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.ArrayList;
import java.util.List;

public class WhitelistApplicationModalFactory {

    private final DCBotConfig config;

    public WhitelistApplicationModalFactory(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public Modal create() {
        List<ActionRow> rows = new ArrayList<>();
        List<DCBotConfig.ApplicationField> fields = config.getApplicationFields();
        int max = Math.min(fields.size(), 5);

        for (int i = 0; i < max; i++) {
            DCBotConfig.ApplicationField field = fields.get(i);

            TextInput input = TextInput.create(
                            "field:" + field.key(),
                            field.label(),
                            parseStyle(field.style())
                    )
                    .setPlaceholder(field.placeholder())
                    .setRequired(field.required())
                    .setRequiredRange(field.minLength(), field.maxLength())
                    .build();

            rows.add(ActionRow.of(input));
        }

        return Modal.create("whitelist:application", config.getWhitelistFormTitle())
                .addComponents(rows)
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