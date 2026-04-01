package me.lekkernakkie.lekkeradmin.punishment.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
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

    public BanlistCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.punishmentService = plugin.getPunishmentService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lekkeradmin.punishment.banlist")) {
            sender.sendMessage(plugin.lang().message(
                    "general.no-permission",
                    "&cDaar edde gij het lef ni vur.."
            ));
            return true;
        }

        int page = 1;

        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(plugin.lang().message(
                        "punishments.banlist.invalid-page",
                        "&cOngeldige pagina."
                ));
                return true;
            }
        }

        if (page <= 0) {
            sender.sendMessage(plugin.lang().message(
                    "punishments.banlist.invalid-page",
                    "&cOngeldige pagina."
            ));
            return true;
        }

        final int requestedPage = page;

        punishmentService.getBanPageCountAsync().thenCompose(maxPage -> {
            if (requestedPage > maxPage) {
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> sender.sendMessage(plugin.lang().message(
                                "punishments.banlist.invalid-page",
                                "&cOngeldige pagina."
                        )));
                return CompletableFuture.completedFuture(null);
            }

            return punishmentService.getActiveBansPageAsync(requestedPage).thenApply(entries -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (entries == null || entries.isEmpty()) {
                        sender.sendMessage(plugin.lang().message(
                                "punishments.banlist.empty",
                                "&7Er zijn momenteel geen actieve bans."
                        ));
                        return;
                    }

                    sender.sendMessage(plugin.lang().get(
                            "punishments.banlist.header",
                            "&8&m----------------------------------------"
                    ));

                    sender.sendMessage(plugin.lang().format(
                            "punishments.banlist.title",
                            "&9Actieve bans &7(Pagina &b{page}&7/&b{max_page}&7)",
                            java.util.Map.of(
                                    "page", String.valueOf(requestedPage),
                                    "max_page", String.valueOf(maxPage)
                            )
                    ));

                    int startIndex = (requestedPage - 1) * plugin.getConfigManager().getPunishmentsConfig().getBanlistEntriesPerPage();

                    for (int i = 0; i < entries.size(); i++) {
                        PunishmentEntry entry = entries.get(i);

                        String duration = PunishmentFormatter.formatDuration(entry.getDurationMs());
                        String expiresAt = PunishmentFormatter.formatDate(
                                entry.getExpiresAt(),
                                plugin.getConfigManager().getPunishmentsConfig().getDateFormat(),
                                plugin.getConfigManager().getPunishmentsConfig().getTimezone()
                        );

                        String entryLine = plugin.lang().format(
                                "punishments.banlist.entry",
                                "&7#&b{index} &b{player} &7- &b{reason}",
                                java.util.Map.of(
                                        "index", String.valueOf(startIndex + i + 1),
                                        "player", PunishmentFormatter.valueOrUnknown(entry.getMinecraftName()),
                                        "reason", PunishmentFormatter.valueOrUnknown(entry.getReason())
                                )
                        );

                        String subEntryLine = plugin.lang().format(
                                "punishments.banlist.sub-entry",
                                "&7Door: &b{actor} &7| Duur: &b{duration} &7| Tot: &b{expires_at}",
                                java.util.Map.of(
                                        "actor", PunishmentFormatter.valueOrUnknown(entry.getIssuedByName()),
                                        "duration", duration,
                                        "expires_at", expiresAt
                                )
                        );

                        sender.sendMessage(entryLine);
                        sender.sendMessage(subEntryLine);
                    }

                    sender.sendMessage(plugin.lang().get(
                            "punishments.banlist.footer",
                            "&8&m----------------------------------------"
                    ));
                });
                return null;
            });
        }).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> sender.sendMessage(plugin.lang().message(
                            "punishments.banlist.load-failed",
                            "&cEr ging iets mis bij het laden van de banlist."
                    )));
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
}