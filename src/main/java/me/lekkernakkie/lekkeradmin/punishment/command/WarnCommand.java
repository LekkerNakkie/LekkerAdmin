package me.lekkernakkie.lekkeradmin.punishment.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.punishment.service.WarnService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WarnCommand implements CommandExecutor, TabCompleter {

    private final LekkerAdmin plugin;
    private final WarnService warnService;

    public WarnCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.warnService = plugin.getWarnService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.warn")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.lang().message(
                    "punishments.warn.usage",
                    "&7Gebruik: &b/warn <speler> [reden]"
            ));
            return true;
        }

        String targetName = args[0];
        String reason = null;

        if (args.length >= 2) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        warnService.warnAsync(sender, targetName, reason)
                .thenAccept(result -> {
                    if (!result.success()) {
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> sender.sendMessage(result.message()));
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> sender.sendMessage(plugin.lang().message(
                                    "punishments.warn.async-failed",
                                    "&cEr ging iets mis bij het warnen."
                            )));
                    plugin.debug("Warn async error: " + throwable.getMessage());
                    return null;
                });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.warn")) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}