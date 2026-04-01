package me.lekkernakkie.lekkeradmin.punishment.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.repository.PunishmentRepository;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentStatus;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClearHistoryCommand implements CommandExecutor, TabCompleter {

    public static final String HISTORY_CLEAR_PREFIX = "[HISTORY_CLEAR]";

    private final LekkerAdmin plugin;
    private final PunishmentRepository repository;

    public ClearHistoryCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.repository = new PunishmentRepository(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var config = plugin.getConfigManager().getPunishmentsConfig();

        if (!sender.hasPermission("lekkeradmin.punishment.clearhistory")) {
            sender.sendMessage(color(config.getNoPermissionMessage()));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(color(config.getClearHistoryUsageMessage()));
            return true;
        }

        String targetName = args[0];
        String targetArg = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();

        if (reason.isBlank()) {
            sender.sendMessage(color(config.getReasonRequiredMessage()));
            return true;
        }

        String storedReason = HISTORY_CLEAR_PREFIX + " " + reason;

        plugin.getDatabaseManager().runAsync(() -> {
            if (targetArg.equalsIgnoreCase("all")) {
                List<PunishmentEntry> entries = repository.findAllVisiblePunishmentsByMinecraftName(targetName);

                int changed = 0;
                for (PunishmentEntry entry : entries) {
                    if (entry.getStatus() == PunishmentStatus.ACTIVE) {
                        continue;
                    }

                    repository.markHistoryRemoved(
                            entry.getId(),
                            System.currentTimeMillis(),
                            sender.getName(),
                            getActorUuid(sender),
                            storedReason
                    );
                    changed++;
                }

                int finalChanged = changed;

                if (finalChanged > 0 && plugin.getDiscordPunishmentLogger() != null) {
                    plugin.getDiscordPunishmentLogger().logHistoryClearAll(
                            sender.getName(),
                            targetName,
                            finalChanged,
                            reason
                    );
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalChanged <= 0) {
                        sender.sendMessage(color(config.getClearHistoryNoEntriesMessage()
                                .replace("{player}", targetName)));
                        return;
                    }

                    sender.sendMessage(color(config.getClearHistoryAllSuccessMessage()
                            .replace("{amount}", String.valueOf(finalChanged))
                            .replace("{suffix}", finalChanged == 1 ? "y" : "ies")
                            .replace("{player}", targetName)));
                });
                return;
            }

            long punishmentId;
            try {
                punishmentId = Long.parseLong(targetArg);
            } catch (NumberFormatException ex) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(color(config.getClearHistoryIdInvalidMessage()))
                );
                return;
            }

            Optional<PunishmentEntry> optionalEntry = repository.findPunishmentById(punishmentId);

            if (optionalEntry.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(color(config.getClearHistoryEntryNotFoundMessage()
                                .replace("{id}", String.valueOf(punishmentId))))
                );
                return;
            }

            PunishmentEntry entry = optionalEntry.get();

            if (entry.getMinecraftName() == null || !entry.getMinecraftName().equalsIgnoreCase(targetName)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(color(config.getClearHistoryWrongPlayerMessage()
                                .replace("{player}", targetName)))
                );
                return;
            }

            if (entry.getStatus() == PunishmentStatus.ACTIVE) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(color(config.getClearHistoryActiveBlockedMessage()))
                );
                return;
            }

            if (entry.getRemoveReason() != null && entry.getRemoveReason().startsWith(HISTORY_CLEAR_PREFIX)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(color(config.getClearHistoryAlreadyHiddenMessage()))
                );
                return;
            }

            repository.markHistoryRemoved(
                    entry.getId(),
                    System.currentTimeMillis(),
                    sender.getName(),
                    getActorUuid(sender),
                    storedReason
            );

            if (plugin.getDiscordPunishmentLogger() != null) {
                plugin.getDiscordPunishmentLogger().logHistoryClearSingle(
                        sender.getName(),
                        targetName,
                        entry,
                        reason
                );
            }

            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(color(config.getClearHistorySingleSuccessMessage()
                            .replace("{id}", String.valueOf(entry.getId()))
                            .replace("{player}", targetName)))
            );
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.clearhistory")) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String targetName = args[0];
            String input = args[1].toLowerCase(Locale.ROOT);

            try {
                List<String> options = repository.findVisiblePunishmentIdsByMinecraftName(targetName).stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());

                options.add(0, "all");

                return options.stream()
                        .distinct()
                        .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(input))
                        .collect(Collectors.toList());
            } catch (Exception ex) {
                return List.of("all").stream()
                        .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(input))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }

    private String getActorUuid(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId().toString();
        }
        return null;
    }

    private String color(String input) {
        return PunishmentFormatter.colorize(input);
    }
}