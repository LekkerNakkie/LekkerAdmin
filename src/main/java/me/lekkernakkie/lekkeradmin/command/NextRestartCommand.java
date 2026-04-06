package me.lekkernakkie.lekkeradmin.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.RestartService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class NextRestartCommand implements CommandExecutor {

    private final LekkerAdmin plugin;

    public NextRestartCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!sender.hasPermission("lekkeradmin.restart") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        RestartService restartService = plugin.getRestartService();
        List<RestartService.UpcomingRestart> upcoming = restartService.getUpcomingRestarts(3);

        sender.sendMessage(plugin.lang().get("nextrestart.header", "&8&m----------------------------------------"));
        sender.sendMessage(plugin.lang().get("nextrestart.title", "&9Volgende Restarts"));

        if (upcoming.isEmpty()) {
            sender.sendMessage(plugin.lang().get("nextrestart.none", "&7Er zijn momenteel geen komende restarts gevonden."));
            sender.sendMessage(plugin.lang().get("nextrestart.footer", "&8&m----------------------------------------"));
            return true;
        }

        int index = 1;
        for (RestartService.UpcomingRestart entry : upcoming) {
            sender.sendMessage(plugin.lang().format(
                    "nextrestart.entry",
                    "&7#&b{index} &8- &b{type} &8- &7{when} &8- &b{remaining}",
                    Map.of(
                            "index", String.valueOf(index),
                            "type", entry.manual() ? "Manual" : "Auto",
                            "when", restartService.formatTargetTime(entry.time()),
                            "remaining", restartService.formatDurationUntil(entry.time())
                    )
            ));

            sender.sendMessage(plugin.lang().format(
                    "nextrestart.entry-reason",
                    "&8  &7Reden: &b{reason}",
                    Map.of("reason", entry.reason() == null || entry.reason().isBlank() ? "-" : entry.reason())
            ));

            index++;
        }

        sender.sendMessage(plugin.lang().get("nextrestart.footer", "&8&m----------------------------------------"));
        return true;
    }
}