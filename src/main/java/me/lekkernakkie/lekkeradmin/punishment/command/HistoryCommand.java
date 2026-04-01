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
        var config = plugin.getConfigManager().getPunishmentsConfig();

        if (!sender.hasPermission("lekkeradmin.punishment.history")) {
            sender.sendMessage(color(config.getNoPermissionMessage()));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(color(config.getHistoryUsageMessage()));
            return true;
        }

        String targetName = args[0];
        int page = 1;

        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(color(config.getHistoryInvalidPageMessage()));
                return true;
            }
        }

        if (page <= 0) {
            sender.sendMessage(color(config.getHistoryPageTooLowMessage()));
            return true;
        }

        int requestedPage = page;

        plugin.getDatabaseManager().runAsync(() -> {
            int totalEntries = repository.countVisiblePunishmentsByMinecraftName(targetName);
            int totalPages = Math.max(1, (int) Math.ceil(totalEntries / (double) ENTRIES_PER_PAGE));

            if (requestedPage > totalPages && totalEntries > 0) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(color(config.getHistoryPageNotFoundMessage()
                                .replace("{max_page}", String.valueOf(totalPages))))
                );
                return;
            }

            int offset = (requestedPage - 1) * ENTRIES_PER_PAGE;
            List<PunishmentEntry> entries = repository.findVisiblePunishmentsByMinecraftNamePaged(targetName, ENTRIES_PER_PAGE, offset);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (entries.isEmpty()) {
                    sender.sendMessage(color(config.getHistoryEmptyMessage()
                            .replace("{player}", targetName)));
                    return;
                }

                sender.sendMessage(color(config.getHistoryHeaderLine()));
                sender.sendMessage(color(config.getHistoryTitle()
                        .replace("{player}", targetName)
                        .replace("{page}", String.valueOf(requestedPage))
                        .replace("{max_page}", String.valueOf(totalPages))));
                sender.sendMessage(color(config.getHistoryHeaderLine()));

                int globalIndex = offset + 1;

                for (PunishmentEntry entry : entries) {
                    sendEntry(sender, entry, globalIndex);
                    globalIndex++;
                }

                sendPagination(sender, targetName, requestedPage, totalPages);

                sender.sendMessage(color(config.getHistoryHeaderLine()));
            });
        });

        return true;
    }

    private void sendEntry(CommandSender sender, PunishmentEntry entry, int index) {
        var config = plugin.getConfigManager().getPunishmentsConfig();

        String type = entry.getPunishmentType() == null ? "ONBEKEND" : entry.getPunishmentType().name();
        String issuedAt = formatDate(entry.getIssuedAt());
        String expiresAt = formatDate(entry.getExpiresAt());
        String removedAt = formatDate(entry.getRemovedAt());
        String duration = PunishmentFormatter.formatDuration(entry.getDurationMs());

        String statusPart = buildStatusDisplay(entry);
        if (!statusPart.isBlank()) {
            statusPart = " " + color(config.getHistoryStatusLabel().replace("{status}", statusPart));
        }

        String header = config.getHistoryEntryHeader()
                .replace("{index}", String.valueOf(index))
                .replace("{id}", String.valueOf(entry.getId()))
                .replace("{type}", type)
                .replace("{status_part}", statusPart);

        sender.sendMessage(color(header));
        sender.sendMessage(color(config.getHistoryEntryReason()
                .replace("{reason}", safe(entry.getReason()))));
        sender.sendMessage(color(config.getHistoryEntryIssued()
                .replace("{actor}", safe(entry.getIssuedByName()))
                .replace("{issued_at}", issuedAt)));

        if (entry.getDurationMs() != null) {
            sender.sendMessage(color(config.getHistoryEntryDuration()
                    .replace("{duration}", safe(duration))
                    .replace("{expires_at}", expiresAt)));
        }

        if (shouldShowHandledInfo(entry)) {
            sender.sendMessage(color(config.getHistoryEntryHandledBy()
                    .replace("{actor}", resolveRemovedBy(entry))
                    .replace("{removed_at}", removedAt)));
            sender.sendMessage(color(config.getHistoryEntryHandledReason()
                    .replace("{reason}", safe(entry.getRemoveReason()))));
        }

        sender.sendMessage(color(config.getHistoryHeaderLine()));
    }

    private String buildStatusDisplay(PunishmentEntry entry) {
        var config = plugin.getConfigManager().getPunishmentsConfig();

        if (entry == null || entry.getPunishmentType() == null || entry.getStatus() == null) {
            return "";
        }

        PunishmentType type = entry.getPunishmentType();
        String status = entry.getStatus().name().toUpperCase(Locale.ROOT);

        if (type != PunishmentType.BAN && type != PunishmentType.MUTE) {
            return "";
        }

        return switch (status) {
            case "ACTIVE" -> color(config.getHistoryStatusActive());
            case "EXPIRED" -> color(config.getHistoryStatusExpired());
            case "REMOVED" -> color(config.getHistoryStatusRemoved());
            default -> color("&7" + status);
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
        var config = plugin.getConfigManager().getPunishmentsConfig();

        TextComponent root = new TextComponent(color(config.getHistoryPaginationPrefix()));

        for (int page = 1; page <= totalPages; page++) {
            if (page > 1) {
                root.addExtra(new TextComponent(color("&8, ")));
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
                        new Text(config.getHistoryPaginationHover().replace("{page}", String.valueOf(page)))
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

    private String color(String input) {
        return PunishmentFormatter.colorize(input);
    }
}