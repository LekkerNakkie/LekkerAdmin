package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.command.CommandSender;

public class ReloadSubCommand {

    private final LekkerAdmin plugin;

    public ReloadSubCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lekkeradmin.reload") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        try {
            plugin.reloadPlugin();
            sender.sendMessage(plugin.lang().message(
                    "admin.reload-complete",
                    "&7LekkerAdmin is volledig &aherladen&7."
            ));
        } catch (Exception ex) {
            sender.sendMessage(plugin.lang().message(
                    "general.unknown-error",
                    "&cEr is iets misgelopen."
            ));
            plugin.getLogger().warning("Reload command failed: " + ex.getMessage());
            ex.printStackTrace();
        }

        return true;
    }
}