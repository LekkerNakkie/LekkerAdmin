package me.lekkernakkie.lekkeradmin.punishment.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.PunishmentsConfig;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.punishment.service.PunishmentService;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BanlistCommand implements CommandExecutor, TabCompleter {

    private final LekkerAdmin plugin;
    private final PunishmentService punishmentService;
    private final PunishmentsConfig config;

    public BanlistCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.punishmentService = plugin.getPunishmentService();
        this.config = plugin.getConfigManager().getPunishmentsConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.banlist")) {
            sender.sendMessage(color(config.getNoPermissionMessage()));
            return true;
        }

        int page = 1;

        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(color(config.getBanlistInvalidPageMessage()));
                return true;
            }
        }

        if (page <= 0) {
            sender.sendMessage(color(config.getBanlistInvalidPageMessage()));
            return true;
        }

        final int requestedPage = page;

        punishmentService.getBanPageCountAsync().thenCompose(maxPage -> {
            if (requestedPage > maxPage) {
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> sender.sendMessage(color(config.getBanlistInvalidPageMessage())));
                return CompletableFuture.completedFuture(null);
            }

            return punishmentService.getActiveBansPageAsync(requestedPage).thenApply(entries -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (entries == null || entries.isEmpty()) {
                        sender.sendMessage(color(config.getBanlistEmptyMessage()));
                        return;
                    }

                    sender.sendMessage(color(config.getBanlistHeader()));
                    sender.sendMessage(color(
                            config.getBanlistTitle()
                                    .replace("{page}", String.valueOf(requestedPage))
                                    .replace("{max_page}", String.valueOf(maxPage))
                    ));

                    int startIndex = (requestedPage - 1) * config.getBanlistEntriesPerPage();

                    for (int i = 0; i < entries.size(); i++) {
                        PunishmentEntry entry = entries.get(i);

                        String duration = PunishmentFormatter.formatDuration(entry.getDurationMs());
                        String expiresAt = PunishmentFormatter.formatDate(
                                entry.getExpiresAt(),
                                config.getDateFormat(),
                                config.getTimezone()
                        );

                        String entryLine = config.getBanlistEntry()
                                .replace("{index}", String.valueOf(startIndex + i + 1))
                                .replace("{player}", PunishmentFormatter.valueOrUnknown(entry.getMinecraftName()))
                                .replace("{reason}", PunishmentFormatter.valueOrUnknown(entry.getReason()));

                        String subEntryLine = config.getBanlistSubEntry()
                                .replace("{actor}", PunishmentFormatter.valueOrUnknown(entry.getIssuedByName()))
                                .replace("{duration}", duration)
                                .replace("{expires_at}", expiresAt);

                        sender.sendMessage(color(entryLine));
                        sender.sendMessage(color(subEntryLine));
                    }

                    sender.sendMessage(color(config.getBanlistFooter()));
                });
                return null;
            });
        }).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> sender.sendMessage(color("&cEr ging iets mis bij het laden van de banlist.")));
            plugin.debug("Banlist async error: " + throwable.getMessage());
            return null;
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.banlist")) {
            return List.of();
        }

        if (args.length == 1) {
            return IntStream.rangeClosed(1, 10)
                    .mapToObj(String::valueOf)
                    .filter(page -> page.startsWith(args[0]))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private String color(String input) {
        return PunishmentFormatter.colorize(input);
    }
}