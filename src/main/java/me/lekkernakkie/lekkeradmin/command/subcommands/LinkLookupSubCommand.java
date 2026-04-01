package me.lekkernakkie.lekkeradmin.command.sub;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.service.link.LinkLookupService;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class LinkLookupSubCommand {

    private final LinkLookupService linkLookupService;

    public LinkLookupSubCommand(LekkerAdmin plugin) {
        this.linkLookupService = new LinkLookupService(plugin);
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Gebruik: /la linklookup <discord|minecraft> <waarde>");
            return true;
        }

        String type = args[1];
        String value = args[2];

        Optional<DiscordMinecraftLink> result;

        if (type.equalsIgnoreCase("discord")) {
            result = linkLookupService.findByDiscordUserId(value);
        } else if (type.equalsIgnoreCase("minecraft")) {
            result = linkLookupService.findByMinecraftName(value);
        } else {
            sender.sendMessage("Ongeldig type. Gebruik discord of minecraft.");
            return true;
        }

        if (result.isEmpty()) {
            sender.sendMessage("Geen link gevonden.");
            return true;
        }

        DiscordMinecraftLink link = result.get();

        sender.sendMessage("§7=== §bLekkerAdmin Link Lookup §7===");
        sender.sendMessage("§7Discord User ID: §f" + safe(link.getDiscordUserId()));
        sender.sendMessage("§7Discord Tag: §f" + safe(link.getDiscordTag()));
        sender.sendMessage("§7Minecraft Naam: §f" + safe(link.getMinecraftName()));
        sender.sendMessage("§7Minecraft UUID: §f" + safe(link.getMinecraftUuid()));
        sender.sendMessage("§7Application ID: §f" + safe(link.getApplicationId()));
        sender.sendMessage("§7Linked At: §f" + (link.getLinkedAt() == null ? "-" : link.getLinkedAt()));

        return true;
    }

    private String safe(String input) {
        return input == null || input.isBlank() ? "-" : input;
    }
}