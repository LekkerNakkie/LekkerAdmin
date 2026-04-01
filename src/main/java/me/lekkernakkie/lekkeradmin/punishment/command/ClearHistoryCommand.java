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
import java.util.Map;
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
        if (!sender.hasPermission("lekkeradmin.punishment.clearhistory")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.lang().message(
                    "punishments.clearhistory.usage",
                    "&7Gebruik: &b/clearhistory <speler> <punishmentID/all> <reden>"
            ));
            return true;
        }

        String targetName = args[0];
        String targetArg = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();

        if (reason.isBlank()) {
            sender.sendMessage(plugin.lang().message(
                    "punishments.clearhistory.reason-required",
                    "&cJe moet een reden opgeven."
            ));
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
                        sender.sendMessage(plugin.lang().formatMessage(
                                "punishments.clearhistory.no-entries",
                                "&cGeen history entries gevonden om te clearen voor &b{player}&c.",
                                Map.of("player", targetName)
                        ));
                        return;
                    }

                    sender.sendMessage(plugin.lang().formatMessage(
                            "punishments.clearhistory.all-success",
                            "&7{amount} history entr{suffix} verborgen voor &b{player}&7.",
                            Map.of(
                                    "amount", String.valueOf(finalChanged),
                                    "suffix", finalChanged == 1 ? "y" : "ies",
                                    "player", targetName
                            )
                    ));
                });
                return;
            }

            long punishmentId;
            try {
                punishmentId = Long.parseLong(targetArg);
            } catch (NumberFormatException ex) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.lang().message(
                                "punishments.clearhistory.invalid-id",
                                "&cPunishment ID moet een nummer zijn of &ball&c."
                        ))
                );
                return;
            }

            Optional<PunishmentEntry> optionalEntry = repository.findPunishmentById(punishmentId);

            if (optionalEntry.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.lang().formatMessage(
                                "punishments.clearhistory.entry-not-found",
                                "&cGeen punishment gevonden met ID &b{id}&c.",
                                Map.of("id", String.valueOf(punishmentId))
                        ))
                );
                return;
            }

            PunishmentEntry entry = optionalEntry.get();

            if (entry.getMinecraftName() == null || !entry.getMinecraftName().equalsIgnoreCase(targetName)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.lang().formatMessage(
                                "punishments.clearhistory.wrong-player",
                                "&cDit punishment ID hoort niet bij speler &b{player}&c.",
                                Map.of("player", targetName)
                        ))
                );
                return;
            }

            if (entry.getStatus() == PunishmentStatus.ACTIVE) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.lang().message(
                                "punishments.clearhistory.active-blocked",
                                "&cJe kan geen actieve punishment clearen&7. Gebruik eerst &bunban&7 of &bunmute&7 indien nodig."
                        ))
                );
                return;
            }

            if (entry.getRemoveReason() != null && entry.getRemoveReason().startsWith(HISTORY_CLEAR_PREFIX)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.lang().message(
                                "punishments.clearhistory.already-hidden",
                                "&cDeze history entry is al verborgen."
                        ))
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
                    sender.sendMessage(plugin.lang().formatMessage(
                            "punishments.clearhistory.single-success",
                            "&7Punishment ID &b{id} &7is verborgen uit history voor &b{player}&7.",
                            Map.of(
                                    "id", String.valueOf(entry.getId()),
                                    "player", targetName
                            )
                    ))
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
}