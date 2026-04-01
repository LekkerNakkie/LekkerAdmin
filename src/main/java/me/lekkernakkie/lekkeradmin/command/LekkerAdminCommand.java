package me.lekkernakkie.lekkeradmin.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.command.subcommands.CancelRestartSubCommand;
import me.lekkernakkie.lekkeradmin.command.subcommands.EnderChestCommand;
import me.lekkernakkie.lekkeradmin.command.subcommands.InvseeCommand;
import me.lekkernakkie.lekkeradmin.command.subcommands.PlanRestartSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class LekkerAdminCommand implements CommandExecutor {

    private final LekkerAdmin plugin;
    private final InvseeCommand invseeCommand;
    private final EnderChestCommand enderChestCommand;
    private final PlanRestartSubCommand planRestartCommand;
    private final CancelRestartSubCommand cancelRestartCommand;

    public LekkerAdminCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.invseeCommand = new InvseeCommand(plugin, plugin.getInvseeService());
        this.enderChestCommand = new EnderChestCommand(plugin, plugin.getInvseeService());
        this.planRestartCommand = new PlanRestartSubCommand(plugin);
        this.cancelRestartCommand = new CancelRestartSubCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lekkeradmin.admin") && !sender.hasPermission("lekkeradmin.help")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help" -> sendHelp(sender);
            case "tools" -> sendToolsHelp(sender);
            case "punishments" -> sendPunishmentHelp(sender);
            case "reload" -> handleReload(sender);
            case "invsee" -> invseeCommand.execute(sender, args);
            case "enderchest", "echest" -> enderChestCommand.execute(sender, args);
            case "planrestart" -> planRestartCommand.execute(sender, args);
            case "cancelrestart" -> cancelRestartCommand.execute(sender, args);
            case "maintenancemode", "maintenance" -> handleMaintenanceToggle(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.lang().get("admin.help.header", "&8&m----------------------------------------"));
        sender.sendMessage(plugin.lang().get("admin.help.title", "&9LekkerAdmin &7- Help"));
        sender.sendMessage(plugin.lang().get("admin.help.help", "&7/la help &8- &bToon help"));
        sender.sendMessage(plugin.lang().get("admin.help.reload", "&7/la reload &8- &bHerlaad plugin"));
        sender.sendMessage(plugin.lang().get("admin.help.punishments", "&7/la punishments &8- &bToon punishment commands"));
        sender.sendMessage(plugin.lang().get("admin.help.tools", "&7/la tools &8- &bToon tools commands"));
        sender.sendMessage(plugin.lang().get("admin.help.footer", "&8&m----------------------------------------"));
    }

    private void sendToolsHelp(CommandSender sender) {
        sender.sendMessage(plugin.lang().get("admin.tools.header", "&8&m----------------------------------------"));
        sender.sendMessage(plugin.lang().get("admin.tools.title", "&9LekkerAdmin &7- Tools"));

        if (sender.hasPermission("lekkeradmin.invsee") || sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().get("admin.tools.invsee", "&7/la invsee <speler> &8- &bBekijk inventory van speler"));
        }

        if (sender.hasPermission("lekkeradmin.enderchest") || sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().get("admin.tools.enderchest", "&7/la enderchest <speler> &8- &bBekijk enderchest van speler"));
        }

        if (sender.hasPermission("lekkeradmin.restart") || sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().get("admin.tools.planrestart", "&7/la planrestart <time> <reden...> &8- &bPlan een restart"));
            sender.sendMessage(plugin.lang().get("admin.tools.cancelrestart", "&7/la cancelrestart &8- &bAnnuleer geplande restart"));
        }

        if (sender.hasPermission("lekkeradmin.maintenance") || sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().get("admin.tools.maintenance", "&7/la maintenancemode &8- &bToggle maintenance mode"));
        }

        sender.sendMessage(plugin.lang().get("admin.tools.footer", "&8&m----------------------------------------"));
    }

    private void sendPunishmentHelp(CommandSender sender) {
        sender.sendMessage(plugin.lang().get("admin.punishment-help.header", "&8&m----------------------------------------"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.title", "&9Punishments &7- Commands"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.ban", "&7/ban <speler> <tijd|perm> [reden]"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.unban", "&7/unban <speler> [reden]"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.mute", "&7/mute <speler> <tijd|perm> [reden]"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.unmute", "&7/unmute <speler> [reden]"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.kick", "&7/kick <speler> [reden]"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.warn", "&7/warn <speler> [reden]"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.banlist", "&7/banlist [pagina]"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.history", "&7/history <speler> [pagina]"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.clearhistory", "&7/clearhistory <speler> <punishmentID/all> [reden]"));
        sender.sendMessage(plugin.lang().get("admin.punishment-help.footer", "&8&m----------------------------------------"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("lekkeradmin.admin") && !sender.hasPermission("lekkeradmin.reload")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return;
        }

        plugin.reloadPlugin();
        sender.sendMessage(plugin.lang().message(
                "admin.reload-complete",
                "&7LekkerAdmin is volledig &aherladen&7."
        ));
    }

    private void handleMaintenanceToggle(CommandSender sender) {
        if (!sender.hasPermission("lekkeradmin.maintenance") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.getMaintenanceService().getNoPermissionMessage());
            return;
        }

        boolean enabled = plugin.getMaintenanceService().toggle();

        if (enabled) {
            sender.sendMessage(plugin.getMaintenanceService().getToggleOnMessage());
            plugin.getMaintenanceService().kickNonBypassOnlinePlayers();
        } else {
            sender.sendMessage(plugin.getMaintenanceService().getToggleOffMessage());
        }
    }
}