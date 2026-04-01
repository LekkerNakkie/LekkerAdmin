package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class PlanRestartSubCommand {

    private final LekkerAdmin plugin;

    public PlanRestartSubCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lekkeradmin.restart") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.lang().message(
                    "restart.usage",
                    "&7Gebruik: &b/planrestart <time> <reden...>"
            ));
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
}