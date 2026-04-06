package me.lekkernakkie.lekkeradmin.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.command.subcommands.CancelRestartSubCommand;
import me.lekkernakkie.lekkeradmin.command.subcommands.PlanRestartSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RestartAliasCommand implements CommandExecutor, TabCompleter {

    private final PlanRestartSubCommand planRestartCommand;
    private final CancelRestartSubCommand cancelRestartCommand;

    public RestartAliasCommand(LekkerAdmin plugin) {
        this.planRestartCommand = new PlanRestartSubCommand(plugin);
        this.cancelRestartCommand = new CancelRestartSubCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("planrestart")) {
            String[] forwarded = new String[args.length + 1];
            forwarded[0] = "planrestart";
            System.arraycopy(args, 0, forwarded, 1, args.length);
            return planRestartCommand.execute(sender, forwarded);
        }

        if (command.getName().equalsIgnoreCase("cancelrestart")) {
            String[] forwarded = new String[args.length + 1];
            forwarded[0] = "cancelrestart";
            System.arraycopy(args, 0, forwarded, 1, args.length);
            return cancelRestartCommand.execute(sender, forwarded);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("lekkeradmin.restart") && !sender.hasPermission("lekkeradmin.admin")) {
            return completions;
        }

        if (command.getName().equalsIgnoreCase("planrestart") && args.length == 1) {
            addIfMatches(completions, args[0], "10m");
            addIfMatches(completions, args[0], "30m");
            addIfMatches(completions, args[0], "1h");
            addIfMatches(completions, args[0], "2h");
            addIfMatches(completions, args[0], "1h30m");
            addIfMatches(completions, args[0], "1d");
        }

        if (command.getName().equalsIgnoreCase("cancelrestart") && args.length == 1) {
            addIfMatches(completions, args[0], "1");
            addIfMatches(completions, args[0], "2");
            addIfMatches(completions, args[0], "3");
        }

        return completions;
    }

    private void addIfMatches(List<String> completions, String input, String option) {
        if (option.toLowerCase().startsWith(input.toLowerCase())) {
            completions.add(option);
        }
    }
}