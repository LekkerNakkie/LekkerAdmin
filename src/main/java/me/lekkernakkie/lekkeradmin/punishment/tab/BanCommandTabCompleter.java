package me.lekkernakkie.lekkeradmin.punishment.tab;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BanCommandTabCompleter extends PunishmentTabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {

        if (args.length == 2) {
            String input = args[1].toLowerCase();
            List<String> suggestions = List.of("10m", "30m", "1h", "6h", "12h", "1d", "3d", "7d", "perm");
            List<String> completions = new ArrayList<>();

            for (String suggestion : suggestions) {
                if (suggestion.toLowerCase().startsWith(input)) {
                    completions.add(suggestion);
                }
            }

            return completions;
        }

        return super.onTabComplete(sender, command, alias, args);
    }
}