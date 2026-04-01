package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import me.lekkernakkie.lekkeradmin.service.application.ApplicationService;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WhitelistSubCommand {

    private final LekkerAdmin plugin;
    private final ApplicationService applicationService;

    public WhitelistSubCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.applicationService = new ApplicationService(plugin);
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lekkeradmin.whitelist") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (args.length < 3) {
            sendUsage(sender);
            return true;
        }

        String action = args[1];

        if (action.equalsIgnoreCase("status")) {
            String applicationId = args[2];
            Optional<WhitelistApplication> optional = applicationService.findByApplicationId(applicationId);

            if (optional.isEmpty()) {
                sender.sendMessage(plugin.lang().withPrefix("&cAanvraag niet gevonden."));
                return true;
            }

            WhitelistApplication application = optional.get();
            sender.sendMessage(plugin.lang().withPrefix("&7Application ID: &b" + safe(application.getApplicationId())));
            sender.sendMessage("&7Discord: &b" + safe(application.getDiscordTag()) + " &8(" + safe(application.getDiscordUserId()) + "&8)");
            sender.sendMessage("&7Minecraft: &b" + safe(application.getMinecraftName()));
            sender.sendMessage("&7Status: &b" + application.getStatus().name());
            sender.sendMessage("&7Retries: &b" + application.getNameRetryCount());
            sender.sendMessage("&7Reviewer: &b" + safe(application.getReviewedByDiscordId()));
            sender.sendMessage("&7Reden: &b" + safe(application.getReviewReason()));
            return true;
        }

        sendUsage(sender);
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            addIfMatches(completions, args[1], "status");
        }

        return completions;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(plugin.lang().withPrefix("&7Gebruik: &b/lekkeradmin whitelist status <applicationId>"));
    }

    private void addIfMatches(List<String> completions, String input, String option) {
        if (option.toLowerCase().startsWith(input.toLowerCase())) {
            completions.add(option);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}