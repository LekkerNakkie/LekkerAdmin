package me.lekkernakkie.lekkeradmin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EnderChestAliasCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        StringBuilder builder = new StringBuilder("lekkeradmin enderchest");
        for (String arg : args) {
            builder.append(" ").append(arg);
        }
        return sender.getServer().dispatchCommand(sender, builder.toString());
    }
}