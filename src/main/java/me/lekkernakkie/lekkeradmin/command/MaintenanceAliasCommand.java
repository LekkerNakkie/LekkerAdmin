package me.lekkernakkie.lekkeradmin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MaintenanceAliasCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean success = Bukkit.dispatchCommand(sender, "lekkeradmin maintenancemode");
        if (!success) {
            sender.sendMessage("§cKon /maintenance niet doorsturen naar /lekkeradmin maintenancemode.");
        }
        return true;
    }
}