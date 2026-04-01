package me.lekkernakkie.lekkeradmin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LekkerAdminCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("lekkeradmin.help") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "help");
                addIfMatches(completions, args[0], "punishments");
                addIfMatches(completions, args[0], "tools");
            }

            if (sender.hasPermission("lekkeradmin.reload") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "reload");
            }

            if (sender.hasPermission("lekkeradmin.invsee") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "invsee");
            }

            if (sender.hasPermission("lekkeradmin.enderchest") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "enderchest");
                addIfMatches(completions, args[0], "echest");
            }

            if (sender.hasPermission("lekkeradmin.restart") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "planrestart");
                addIfMatches(completions, args[0], "cancelrestart");
            }

            if (sender.hasPermission("lekkeradmin.maintenance") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "maintenancemode");
                addIfMatches(completions, args[0], "maintenance");
            }

            if (sender.hasPermission("lekkeradmin.punishment.ban") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "ban");
            }

            if (sender.hasPermission("lekkeradmin.punishment.unban") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "unban");
            }

            if (sender.hasPermission("lekkeradmin.punishment.mute") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "mute");
            }

            if (sender.hasPermission("lekkeradmin.punishment.unmute") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "unmute");
            }

            if (sender.hasPermission("lekkeradmin.punishment.kick") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "kick");
            }

            if (sender.hasPermission("lekkeradmin.punishment.warn") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "warn");
            }

            if (sender.hasPermission("lekkeradmin.punishment.banlist") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "banlist");
            }

            if (sender.hasPermission("lekkeradmin.discord.playerinfo") || sender.hasPermission("lekkeradmin.admin")) {
                addIfMatches(completions, args[0], "playerinfo");
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invsee")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                addIfMatches(completions, args[1], player.getName());
            }
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("enderchest") || args[0].equalsIgnoreCase("echest"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                addIfMatches(completions, args[1], player.getName());
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("planrestart")) {
            addIfMatches(completions, args[1], "10m");
            addIfMatches(completions, args[1], "30m");
            addIfMatches(completions, args[1], "1h");
            addIfMatches(completions, args[1], "2h");
            addIfMatches(completions, args[1], "1h30m");
            addIfMatches(completions, args[1], "1d");
        }

        return completions;
    }

    private void addIfMatches(List<String> completions, String input, String option) {
        if (option.toLowerCase().startsWith(input.toLowerCase())) {
            completions.add(option);
        }
    }
}