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
            sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getPrefix() + "&cGeen permissie."));
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
                sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getPrefix() + "&cAanvraag niet gevonden."));
                return true;
            }

            WhitelistApplication application = optional.get();
            sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getPrefix() + "&fApplication ID: &b" + safe(application.getApplicationId())));
            sender.sendMessage(color("&7Discord: &f" + safe(application.getDiscordTag()) + " &8(" + safe(application.getDiscordUserId()) + "&8)"));
            sender.sendMessage(color("&7Minecraft: &f" + safe(application.getMinecraftName())));
            sender.sendMessage(color("&7Status: &f" + application.getStatus().name()));
            sender.sendMessage(color("&7Retries: &f" + application.getNameRetryCount()));
            sender.sendMessage(color("&7Reviewer: &f" + safe(application.getReviewedByDiscordId())));
            sender.sendMessage(color("&7Reden: &f" + safe(application.getReviewReason())));
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
        sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getPrefix() + "&eGebruik: &7/lekkeradmin whitelist status <applicationId>"));
    }

    private void addIfMatches(List<String> completions, String input, String option) {
        if (option.toLowerCase().startsWith(input.toLowerCase())) {
            completions.add(option);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String color(String input) {
        return input == null ? "" : input.replace("&", "§");
    }
}