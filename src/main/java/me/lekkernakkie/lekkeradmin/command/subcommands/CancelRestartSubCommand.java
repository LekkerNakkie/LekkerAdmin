package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.command.CommandSender;

public class CancelRestartSubCommand {

    private final LekkerAdmin plugin;

    public CancelRestartSubCommand(LekkerAdmin plugin) {
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

        int index = 1;

        if (args.length >= 2) {
            try {
                index = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(plugin.lang().message(
                        "restart.cancel-invalid-index",
                        "&cOngeldige restart index. Gebruik bv: &b/cancelrestart 2"
                ));
                return true;
            }
        }

        plugin.getRestartService().cancelUpcomingRestart(sender, index);
        return true;
    }
}