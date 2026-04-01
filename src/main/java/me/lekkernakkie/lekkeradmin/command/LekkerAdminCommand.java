package me.lekkernakkie.lekkeradmin.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.command.subcommands.CancelRestartSubCommand;
import me.lekkernakkie.lekkeradmin.command.subcommands.EnderChestCommand;
import me.lekkernakkie.lekkeradmin.command.subcommands.InvseeCommand;
import me.lekkernakkie.lekkeradmin.command.subcommands.PlanRestartSubCommand;
import me.lekkernakkie.lekkeradmin.config.MainConfig;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class LekkerAdminCommand implements CommandExecutor {

    private final LekkerAdmin plugin;
    private final MainConfig mainConfig;
    private final InvseeCommand invseeCommand;
    private final EnderChestCommand enderChestCommand;
    private final PlanRestartSubCommand planRestartCommand;
    private final CancelRestartSubCommand cancelRestartCommand;

    public LekkerAdminCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.mainConfig = plugin.getConfigManager().getMainConfig();
        this.invseeCommand = new InvseeCommand(plugin, plugin.getInvseeService());
        this.enderChestCommand = new EnderChestCommand(plugin, plugin.getInvseeService());
        this.planRestartCommand = new PlanRestartSubCommand(plugin);
        this.cancelRestartCommand = new CancelRestartSubCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lekkeradmin.admin") && !sender.hasPermission("lekkeradmin.help")) {
            sender.sendMessage(color("&cDaar edde gij het lef ni vur.."));
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
        sender.sendMessage(color("&8&m----------------------------------------"));
        sender.sendMessage(color("&bLekkerAdmin &7- Help"));
        sender.sendMessage(color("&7/la help &8- &fToon help"));
        sender.sendMessage(color("&7/la reload &8- &fHerlaad plugin"));
        sender.sendMessage(color("&7/la punishments &8- &fToon punishment commands"));
        sender.sendMessage(color("&7/la tools &8- &fToon tools commands"));
        sender.sendMessage(color("&8&m----------------------------------------"));
    }

    private void sendToolsHelp(CommandSender sender) {
        sender.sendMessage(color("&8&m----------------------------------------"));
        sender.sendMessage(color("&bLekkerAdmin &7- Tools"));

        if (sender.hasPermission("lekkeradmin.invsee") || sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(color("&7/la invsee <speler> &8- &fBekijk inventory van speler"));
        }

        if (sender.hasPermission("lekkeradmin.enderchest") || sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(color("&7/la enderchest <speler> &8- &fBekijk enderchest van speler"));
        }

        if (sender.hasPermission("lekkeradmin.restart") || sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(color("&7/la planrestart <time> <reden...> &8- &fPlan een restart"));
            sender.sendMessage(color("&7/la cancelrestart &8- &fAnnuleer geplande restart"));
        }

        if (sender.hasPermission("lekkeradmin.maintenance") || sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(color("&7/la maintenancemode &8- &fToggle maintenance mode"));
        }

        sender.sendMessage(color("&8&m----------------------------------------"));
    }

    private void sendPunishmentHelp(CommandSender sender) {
        sender.sendMessage(color("&8&m----------------------------------------"));
        sender.sendMessage(color("&cPunishments &7- Commands"));
        sender.sendMessage(color("&7/ban <speler> <tijd|perm> [reden]"));
        sender.sendMessage(color("&7/unban <speler> [reden]"));
        sender.sendMessage(color("&7/mute <speler> <tijd|perm> [reden]"));
        sender.sendMessage(color("&7/unmute <speler> [reden]"));
        sender.sendMessage(color("&7/kick <speler> [reden]"));
        sender.sendMessage(color("&7/warn <speler> [reden]"));
        sender.sendMessage(color("&7/banlist [pagina]"));
        sender.sendMessage(color("&7/history <speler> [pagina]"));
        sender.sendMessage(color("&7/clearhistory <speler> <punishmentID/all> [reden]"));
        sender.sendMessage(color("&8&m----------------------------------------"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("lekkeradmin.admin") && !sender.hasPermission("lekkeradmin.reload")) {
            sender.sendMessage(color("&cDaar edde gij het lef ni vur.."));
            return;
        }

        plugin.reloadPlugin();
        sender.sendMessage(color(mainConfig.getPrefix() + "&aLekkerAdmin volledig herladen."));
    }

    private void handleMaintenanceToggle(CommandSender sender) {
        if (!sender.hasPermission("lekkeradmin.maintenance") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(color(mainConfig.getMaintenanceNoPermissionMessage()));
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

    private String color(String text) {
        return PunishmentFormatter.colorize(text);
    }
}