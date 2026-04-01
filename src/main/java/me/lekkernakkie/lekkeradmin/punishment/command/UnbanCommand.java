package me.lekkernakkie.lekkeradmin.punishment.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.punishment.service.PunishmentService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UnbanCommand implements CommandExecutor, TabCompleter {

    private final LekkerAdmin plugin;
    private final PunishmentService punishmentService;

    public UnbanCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.punishmentService = plugin.getPunishmentService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.unban")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.lang().message(
                    "punishments.unban.usage",
                    "&7Gebruik: &b/unban <speler> [reden]"
            ));
            return true;
        }

        String targetName = args[0];
        String reason = null;

        if (args.length >= 2) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        punishmentService.unbanAsync(sender, targetName, reason)
                .thenAccept(result -> {
                    if (!result.success()) {
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> sender.sendMessage(result.message()));
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> sender.sendMessage(plugin.lang().message(
                                    "punishments.unban.async-failed",
                                    "&cEr ging iets mis bij het unbannen."
                            )));
                    plugin.debug("Unban async error: " + throwable.getMessage());
                    return null;
                });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.unban")) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .limit(25)
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}