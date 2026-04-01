package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class PlanRestartSubCommand {

    private final LekkerAdmin plugin;

    public PlanRestartSubCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lekkeradmin.restart") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getPlanRestartNoPermissionMessage()));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getPlanRestartUsageMessage()));
            return true;
        }

        String timeInput = args[1];
        String reason;

        if (args.length >= 3) {
            reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        } else {
            reason = plugin.getConfigManager().getMainConfig().getPlanRestartDefaultReason();
        }

        plugin.getRestartService().scheduleManualRestart(sender, timeInput, reason);
        return true;
    }

    private String color(String text) {
        return PunishmentFormatter.colorize(text);
    }
}