package me.lekkernakkie.lekkeradmin.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MaintenanceAliasCommand implements CommandExecutor {

    private final LekkerAdmin plugin;

    public MaintenanceAliasCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean success = Bukkit.dispatchCommand(sender, "lekkeradmin maintenancemode");
        if (!success) {
            sender.sendMessage(plugin.lang().message(
                    "maintenance.alias-forward-failed",
                    "&cKon &b/maintenance&c niet doorsturen naar &b/lekkeradmin maintenancemode&c."
            ));
        }
        return true;
    }
}