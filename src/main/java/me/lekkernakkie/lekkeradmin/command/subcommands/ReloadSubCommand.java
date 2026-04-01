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
            sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getPrefix() + "&cGeen permissie."));
            return true;
        }

        try {
            plugin.getConfigManager().reloadAll();
            sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getPrefix() + "&aConfigs succesvol herladen."));
        } catch (Exception ex) {
            sender.sendMessage(color(plugin.getConfigManager().getMainConfig().getPrefix() + "&cFout bij herladen van configs."));
            plugin.getLogger().warning("Reload command failed: " + ex.getMessage());
            ex.printStackTrace();
        }

        return true;
    }

    private String color(String input) {
        return input == null ? "" : input.replace("&", "§");
    }
}