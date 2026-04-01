package me.lekkernakkie.lekkeradmin.punishment.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.punishment.service.PunishmentService;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentDurationParser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final LekkerAdmin plugin;
    private final PunishmentService punishmentService;

    public MuteCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.punishmentService = plugin.getPunishmentService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.mute")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.lang().message(
                    "punishments.mute.usage",
                    "&7Gebruik: &b/mute <speler> <tijd|perm> [reden]"
            ));
            return true;
        }

        String targetName = args[0];
        String durationInput = args[1];

        PunishmentDurationParser.ParseResult parseResult =
                PunishmentDurationParser.parse(durationInput, plugin.getConfigManager().getPunishmentsConfig());

        if (!parseResult.valid()) {
            sender.sendMessage(plugin.lang().message(
                    "punishments.mute.invalid-duration",
                    "&cOngeldige tijd&7. Gebruik bv: &b10m&7, &b1h&7, &b7d&7, &b1h30m&7, &bperm"
            ));
            return true;
        }

        String reason = null;
        if (args.length >= 3) {
            reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        }

        punishmentService.muteAsync(sender, targetName, parseResult.durationMs(), reason)
                .thenAccept(result -> {
                    if (!result.success()) {
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> sender.sendMessage(result.message()));
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> sender.sendMessage(plugin.lang().message(
                                    "punishments.mute.async-failed",
                                    "&cEr ging iets mis bij het muten."
                            )));
                    plugin.debug("Mute async error: " + throwable.getMessage());
                    return null;
                });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.mute")) {
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

        if (args.length == 2) {
            return List.of("10m", "1h", "1d", "7d", "perm").stream()
                    .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}