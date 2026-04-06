package me.lekkernakkie.lekkeradmin.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.freeze.FreezeService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

public class FreezeCommand implements CommandExecutor {

    private final LekkerAdmin plugin;
    private final FreezeService freezeService;

    public FreezeCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.freezeService = plugin.getFreezeService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lekkeradmin.freeze") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "{prefix} &cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (!freezeService.isEnabled()) {
            sender.sendMessage(plugin.lang().message(
                    "freeze.disabled",
                    "{prefix} &cFreeze staat uitgeschakeld."
            ));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.lang().message(
                    "freeze.usage",
                    "{prefix} &7Gebruik: &b/freeze <speler> [reden]"
            ));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.lang().message(
                    "general.player-not-found",
                    "{prefix} &cSpeler niet gevonden."
            ));
            return true;
        }

        if (freezeService.isFrozen(target)) {
            sender.sendMessage(plugin.lang().message(
                    "freeze.already-frozen",
                    "{prefix} &cDeze speler is al gefreezed."
            ));
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Geen reden opgegeven.";
        Player actor = sender instanceof Player player ? player : null;

        boolean success = freezeService.freeze(actor, target, reason);
        if (!success) {
            sender.sendMessage(plugin.lang().message(
                    "general.unknown-error",
                    "{prefix} &cEr is iets misgelopen."
            ));
            return true;
        }

        sender.sendMessage(plugin.lang().format(
                "freeze.player-frozen",
                "{prefix} &7Speler &b{player} &7is gefreezed.",
                Map.of("player", target.getName())
        ));

        return true;
    }
}