package me.lekkernakkie.lekkeradmin.discord.listener;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.discord.interaction.button.ConfirmButtonHandler;
import me.lekkernakkie.lekkeradmin.discord.interaction.button.ReviewButtonHandler;
import me.lekkernakkie.lekkeradmin.discord.interaction.component.ComponentIdParser;
import me.lekkernakkie.lekkeradmin.discord.interaction.modal.NameRetryModalFactory;
import me.lekkernakkie.lekkeradmin.discord.interaction.modal.ReasonModalFactory;
import me.lekkernakkie.lekkeradmin.discord.interaction.modal.WhitelistApplicationModalFactory;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ButtonListener extends ListenerAdapter {

    private final LekkerAdmin plugin;
    private final WhitelistApplicationModalFactory whitelistApplicationModalFactory;
    private final NameRetryModalFactory nameRetryModalFactory;
    private final ReasonModalFactory reasonModalFactory;
    private final ReviewButtonHandler reviewButtonHandler;
    private final ConfirmButtonHandler confirmButtonHandler;

    public ButtonListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.whitelistApplicationModalFactory = new WhitelistApplicationModalFactory(plugin);
        this.nameRetryModalFactory = new NameRetryModalFactory(plugin);
        this.reasonModalFactory = new ReasonModalFactory();
        this.reviewButtonHandler = new ReviewButtonHandler(plugin);
        this.confirmButtonHandler = new ConfirmButtonHandler(plugin);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        ComponentIdParser.ParsedComponentId parsed = ComponentIdParser.parse(componentId);

        if (parsed == null) {
            return;
        }

        String root = parsed.root();
        String action = parsed.action();

        if (root.equalsIgnoreCase("whitelist") && action.equalsIgnoreCase("start")) {
            event.replyModal(whitelistApplicationModalFactory.create()).queue();
            return;
        }

        if (root.equalsIgnoreCase("whitelist") && action.equalsIgnoreCase("retryname")) {
            String applicationId = parsed.value();
            event.replyModal(nameRetryModalFactory.create(applicationId)).queue();
            return;
        }

        if (root.equalsIgnoreCase("review") && action.equalsIgnoreCase("approvereason")) {
            event.replyModal(reasonModalFactory.createApproveReasonModal(parsed.value())).queue();
            return;
        }

        if (root.equalsIgnoreCase("review") && action.equalsIgnoreCase("denyreason")) {
            event.replyModal(reasonModalFactory.createDenyReasonModal(parsed.value())).queue();
            return;
        }

        if (root.equalsIgnoreCase("review") && (action.equalsIgnoreCase("approve") || action.equalsIgnoreCase("deny"))) {
            reviewButtonHandler.handle(event, parsed);
            return;
        }

        if (root.equalsIgnoreCase("confirm")) {
            confirmButtonHandler.handle(event);
            return;
        }

        plugin.getLogger().info("Unhandled button interaction: " + componentId);
    }
}