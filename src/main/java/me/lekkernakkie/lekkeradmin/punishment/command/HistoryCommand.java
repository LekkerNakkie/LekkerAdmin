package me.lekkernakkie.lekkeradmin.punishment.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.repository.PunishmentRepository;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentType;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoryCommand implements CommandExecutor, TabCompleter {

    private static final int ENTRIES_PER_PAGE = 5;

    private final LekkerAdmin plugin;
    private final PunishmentRepository repository;

    public HistoryCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.repository = new PunishmentRepository(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var punishmentsConfig = plugin.getConfigManager().getPunishmentsConfig();

        if (!sender.hasPermission("lekkeradmin.punishment.history")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.lang().message(
                    "punishments.history.usage",
                    "&7Gebruik: &b/history <speler> [pagina]"
            ));
            return true;
        }

        String targetName = args[0];
        int page = 1;

        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(plugin.lang().message(
                        "punishments.history.invalid-page",
                        "&cPagina moet een nummer zijn."
                ));
                return true;
            }
        }

        if (page <= 0) {
            sender.sendMessage(plugin.lang().message(
                    "punishments.history.page-too-low",
                    "&cPagina moet groter zijn dan &b0&c."
            ));
            return true;
        }

        int requestedPage = page;

        plugin.getDatabaseManager().runAsync(() -> {
            int totalEntries = repository.countVisiblePunishmentsByMinecraftName(targetName);
            int totalPages = Math.max(1, (int) Math.ceil(totalEntries / (double) ENTRIES_PER_PAGE));

            if (requestedPage > totalPages && totalEntries > 0) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.lang().formatMessage(
                                "punishments.history.page-not-found",
                                "&cDie pagina bestaat niet&7. Max pagina: &b{max_page}&7.",
                                Map.of("max_page", String.valueOf(totalPages))
                        ))
                );
                return;
            }

            int offset = (requestedPage - 1) * ENTRIES_PER_PAGE;
            List<PunishmentEntry> entries = repository.findVisiblePunishmentsByMinecraftNamePaged(targetName, ENTRIES_PER_PAGE, offset);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (entries.isEmpty()) {
                    sender.sendMessage(plugin.lang().formatMessage(
                            "punishments.history.empty",
                            "&cGeen punishment history gevonden voor &b{player}&c.",
                            Map.of("player", targetName)
                    ));
                    return;
                }

                sender.sendMessage(plugin.lang().get(
                        "punishments.history.header-line",
                        "&8&m------------------------------------------------"
                ));

                sender.sendMessage(plugin.lang().format(
                        "punishments.history.title",
                        "&9Punishment History &7» &b{player} &7(Pagina &b{page}&7/&b{max_page}&7)",
                        Map.of(
                                "player", targetName,
                                "page", String.valueOf(requestedPage),
                                "max_page", String.valueOf(totalPages)
                        )
                ));

                sender.sendMessage(plugin.lang().get(
                        "punishments.history.header-line",
                        "&8&m------------------------------------------------"
                ));

                int globalIndex = offset + 1;

                for (PunishmentEntry entry : entries) {
                    sendEntry(sender, entry, globalIndex);
                    globalIndex++;
                }

                sendPagination(sender, targetName, requestedPage, totalPages);

                sender.sendMessage(plugin.lang().get(
                        "punishments.history.header-line",
                        "&8&m------------------------------------------------"
                ));
            });
        });

        return true;
    }

    private void sendEntry(CommandSender sender, PunishmentEntry entry, int index) {
        var punishmentsConfig = plugin.getConfigManager().getPunishmentsConfig();

        String type = entry.getPunishmentType() == null ? "ONBEKEND" : entry.getPunishmentType().name();
        String issuedAt = formatDate(entry.getIssuedAt());
        String expiresAt = formatDate(entry.getExpiresAt());
        String removedAt = formatDate(entry.getRemovedAt());
        String duration = PunishmentFormatter.formatDuration(entry.getDurationMs());

        String statusPart = buildStatusDisplay(entry);
        if (!statusPart.isBlank()) {
            statusPart = " " + plugin.lang().format(
                    "punishments.history.status-label",
                    "&7| Status: {status}",
                    Map.of("status", statusPart)
            );
        }

        sender.sendMessage(plugin.lang().format(
                "punishments.history.entry-header",
                "&7#&b{index} &7| ID: &b{id} &7| Type: &b{type}{status_part}",
                Map.of(
                        "index", String.valueOf(index),
                        "id", String.valueOf(entry.getId()),
                        "type", type,
                        "status_part", statusPart
                )
        ));

        sender.sendMessage(plugin.lang().format(
                "punishments.history.entry-reason",
                "&7Reden: &b{reason}",
                Map.of("reason", safe(entry.getReason()))
        ));

        sender.sendMessage(plugin.lang().format(
                "punishments.history.entry-issued",
                "&7Uitgedeeld door: &b{actor} &7| Op: &b{issued_at}",
                Map.of(
                        "actor", safe(entry.getIssuedByName()),
                        "issued_at", issuedAt
                )
        ));

        if (entry.getDurationMs() != null) {
            sender.sendMessage(plugin.lang().format(
                    "punishments.history.entry-duration",
                    "&7Duur: &b{duration} &7| Verloopt: &b{expires_at}",
                    Map.of(
                            "duration", safe(duration),
                            "expires_at", expiresAt
                    )
            ));
        }

        if (shouldShowHandledInfo(entry)) {
            sender.sendMessage(plugin.lang().format(
                    "punishments.history.entry-handled-by",
                    "&7Afgehandeld door: &b{actor} &7| Op: &b{removed_at}",
                    Map.of(
                            "actor", resolveRemovedBy(entry),
                            "removed_at", removedAt
                    )
            ));

            sender.sendMessage(plugin.lang().format(
                    "punishments.history.entry-handled-reason",
                    "&7Afhandel reden: &b{reason}",
                    Map.of("reason", safe(entry.getRemoveReason()))
            ));
        }

        sender.sendMessage(plugin.lang().get(
                "punishments.history.header-line",
                "&8&m------------------------------------------------"
        ));
    }

    private String buildStatusDisplay(PunishmentEntry entry) {
        if (entry == null || entry.getPunishmentType() == null || entry.getStatus() == null) {
            return "";
        }

        PunishmentType type = entry.getPunishmentType();
        String status = entry.getStatus().name().toUpperCase(Locale.ROOT);

        if (type != PunishmentType.BAN && type != PunishmentType.MUTE) {
            return "";
        }

        return switch (status) {
            case "ACTIVE" -> plugin.lang().get("punishments.history.status-active", "&cActief");
            case "EXPIRED" -> plugin.lang().get("punishments.history.status-expired", "&bVerlopen");
            case "REMOVED" -> plugin.lang().get("punishments.history.status-removed", "&aOpgeheven");
            default -> "&7" + status;
        };
    }

    private boolean shouldShowHandledInfo(PunishmentEntry entry) {
        return entry.getRemovedAt() != null || entry.getRemoveReason() != null || entry.getRemovedByName() != null;
    }

    private String resolveRemovedBy(PunishmentEntry entry) {
        String removedBy = safe(entry.getRemovedByName());
        if (!removedBy.equals("-")) {
            return removedBy;
        }

        if (entry.getStatus() != null && entry.getStatus().name().equalsIgnoreCase("EXPIRED")) {
            return "Automatisch";
        }

        if (entry.getRemovedAt() != null) {
            return "Onbekend";
        }

        return "-";
    }

    private void sendPagination(CommandSender sender, String targetName, int currentPage, int totalPages) {
        TextComponent root = new TextComponent(
                plugin.lang().get("punishments.history.pagination-prefix", "&7Pagina's: ")
        );

        for (int page = 1; page <= totalPages; page++) {
            if (page > 1) {
                root.addExtra(new TextComponent("§8, "));
            }

            TextComponent part = new TextComponent("[" + page + "]");

            if (page == currentPage) {
                part.setColor(ChatColor.AQUA);
                part.setBold(true);
            } else {
                part.setColor(ChatColor.GRAY);
                part.setBold(false);
                part.setClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/history " + targetName + " " + page
                ));
                part.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new Text(plugin.lang().format(
                                "punishments.history.pagination-hover",
                                "&7Ga naar pagina &b{page}",
                                Map.of("page", String.valueOf(page))
                        ))
                ));
            }

            root.addExtra(part);
        }

        sender.spigot().sendMessage(root);
    }

    private String formatDate(Long epochMillis) {
        return PunishmentFormatter.formatDate(
                epochMillis,
                plugin.getConfigManager().getPunishmentsConfig().getDateFormat(),
                plugin.getConfigManager().getPunishmentsConfig().getTimezone()
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.history")) {
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

        return List.of();
    }
}