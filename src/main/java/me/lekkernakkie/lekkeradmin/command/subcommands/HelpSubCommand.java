package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpSubCommand {

    private final LekkerAdmin plugin;

    public HelpSubCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getMainConfig().getPrefix();

        List<String> lines = new ArrayList<>();
        lines.add(color(prefix + "&fBeschikbare commands:"));

        if (sender.hasPermission("lekkeradmin.help") || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(color("&7- &b/lekkeradmin help &7Toont deze help."));
        }

        if (sender.hasPermission("lekkeradmin.reload") || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(color("&7- &b/lekkeradmin reload &7Herlaadt config bestanden."));
        }

        if (sender.hasPermission("lekkeradmin.invsee") || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(color("&7- &b/lekkeradmin invsee <speler> &7Bekijk inventory van een speler."));
        }

        if (sender.hasPermission("lekkeradmin.enderchest") || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(color("&7- &b/lekkeradmin enderchest <speler> &7Bekijk enderchest van een speler."));
        }

        if (sender.hasPermission("lekkeradmin.punishment.ban")
                || sender.hasPermission("lekkeradmin.punishment.unban")
                || sender.hasPermission("lekkeradmin.punishment.mute")
                || sender.hasPermission("lekkeradmin.punishment.unmute")
                || sender.hasPermission("lekkeradmin.punishment.kick")
                || sender.hasPermission("lekkeradmin.punishment.warn")
                || sender.hasPermission("lekkeradmin.punishment.banlist")
                || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(color("&7- &b/lekkeradmin punishments &7Toont punishment commands."));
        }

        for (String line : lines) {
            sender.sendMessage(line);
        }

        return true;
    }

    private String color(String input) {
        return input == null ? "" : input.replace("&", "§");
    }
}