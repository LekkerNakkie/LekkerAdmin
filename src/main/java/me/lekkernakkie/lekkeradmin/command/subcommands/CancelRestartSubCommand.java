package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.command.CommandSender;

public class CancelRestartSubCommand {

    private final LekkerAdmin plugin;

    public CancelRestartSubCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lekkeradmin.restart") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getCancelRestartNoPermissionMessage()));
            return true;
        }

        plugin.getRestartService().cancelRestart(sender);
        return true;
    }

    private String color(String text) {
        return PunishmentFormatter.colorize(text);
    }
}