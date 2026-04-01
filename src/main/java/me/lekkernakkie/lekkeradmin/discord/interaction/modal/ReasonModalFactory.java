package me.lekkernakkie.lekkeradmin.discord.interaction.modal;

import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class ReasonModalFactory {

    public Modal createApproveReasonModal(String applicationId) {
        TextInput input = TextInput.create(
                        "field:reason",
                        "Reden van goedkeuring",
                        TextInputStyle.PARAGRAPH
                )
                .setPlaceholder("Optionele reden van goedkeuring")
                .setRequired(false)
                .setRequiredRange(0, 400)
                .build();

        return Modal.create("review:approvereason:" + applicationId, "Goedkeuring met reden")
                .addActionRow(input)
                .build();
    }

    public Modal createDenyReasonModal(String applicationId) {
        TextInput input = TextInput.create(
                        "field:reason",
                        "Reden van afkeuring",
                        TextInputStyle.PARAGRAPH
                )
                .setPlaceholder("Geef de reden van afkeuring in")
                .setRequired(true)
                .setRequiredRange(1, 400)
                .build();

        return Modal.create("review:denyreason:" + applicationId, "Afkeuring met reden")
                .addActionRow(input)
                .build();
    }
}