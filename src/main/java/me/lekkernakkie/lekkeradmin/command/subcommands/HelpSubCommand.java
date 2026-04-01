package me.lekkernakkie.lekkeradmin.command.subcommands;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpSubCommand {

    private final LekkerAdmin plugin;

    public HelpSubCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        List<String> lines = new ArrayList<>();

        lines.add(plugin.lang().get("admin.help.header", "&8&m----------------------------------------"));
        lines.add(plugin.lang().get("admin.help.title", "&9LekkerAdmin &7- Help"));

        if (sender.hasPermission("lekkeradmin.help") || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(plugin.lang().get("admin.help.help", "&7/la help &8- &bToon help"));
        }

        if (sender.hasPermission("lekkeradmin.reload") || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(plugin.lang().get("admin.help.reload", "&7/la reload &8- &bHerlaad plugin"));
        }

        if (sender.hasPermission("lekkeradmin.invsee") || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(plugin.lang().get("admin.tools.invsee", "&7/la invsee <speler> &8- &bBekijk inventory van speler"));
        }

        if (sender.hasPermission("lekkeradmin.enderchest") || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(plugin.lang().get("admin.tools.enderchest", "&7/la enderchest <speler> &8- &bBekijk enderchest van speler"));
        }

        if (sender.hasPermission("lekkeradmin.punishment.ban")
                || sender.hasPermission("lekkeradmin.punishment.unban")
                || sender.hasPermission("lekkeradmin.punishment.mute")
                || sender.hasPermission("lekkeradmin.punishment.unmute")
                || sender.hasPermission("lekkeradmin.punishment.kick")
                || sender.hasPermission("lekkeradmin.punishment.warn")
                || sender.hasPermission("lekkeradmin.punishment.banlist")
                || sender.hasPermission("lekkeradmin.admin")) {
            lines.add(plugin.lang().get("admin.help.punishments", "&7/la punishments &8- &bToon punishment commands"));
        }

        lines.add(plugin.lang().get("admin.help.footer", "&8&m----------------------------------------"));

        for (String line : lines) {
            sender.sendMessage(line);
        }

        return true;
    }
}