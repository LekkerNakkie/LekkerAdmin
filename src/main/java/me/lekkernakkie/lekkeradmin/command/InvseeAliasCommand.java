package me.lekkernakkie.lekkeradmin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class InvseeAliasCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        StringBuilder forwarded = new StringBuilder("lekkeradmin invsee");

        for (String arg : args) {
            forwarded.append(" ").append(arg);
        }

        boolean success = Bukkit.dispatchCommand(sender, forwarded.toString());
        if (!success) {
            sender.sendMessage("§cKon /invsee niet doorsturen naar /lekkeradmin invsee.");
        }

        return true;
    }
}