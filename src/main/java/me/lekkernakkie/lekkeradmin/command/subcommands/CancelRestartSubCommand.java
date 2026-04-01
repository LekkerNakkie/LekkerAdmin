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

        plugin.getRestartService().cancelRestart(sender);
        return true;
    }
}