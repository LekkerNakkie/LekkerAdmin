package me.lekkernakkie.lekkeradmin.command;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.whois.WhoisService;
import me.lekkernakkie.lekkeradmin.service.whois.WhoisService.WhoisProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WhoisCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final LekkerAdmin plugin;
    private final WhoisService whoisService;

    public WhoisCommand(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.whoisService = new WhoisService(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!plugin.getConfigManager().getMainConfig().isWhoisEnabled()) {
            sender.sendMessage(plugin.lang().get("whois.disabled", "&cWhois staat uitgeschakeld."));
            return true;
        }

        if (!sender.hasPermission("lekkeradmin.whois") && !sender.hasPermission("lekkeradmin.admin")) {
            sender.sendMessage(plugin.lang().get("whois.no-permission", "&cDaar edde gij het lef ni vur.."));
            return true;
        }

        if (args.length < 1 || args[0].isBlank()) {
            sender.sendMessage(plugin.lang().get("whois.usage", "&7Gebruik: &b/whois <speler> [pagina]"));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            target = Bukkit.getOfflinePlayer(targetName);
        }

        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            sender.sendMessage(plugin.lang().get("whois.player-not-found", "&cDie speler bestaat niet of heeft nog nooit gejoint."));
            return true;
        }

        int maxPages = Math.max(1, plugin.getConfigManager().getMainConfig().getWhoisMaxPages());
        int page = plugin.getConfigManager().getMainConfig().getWhoisDefaultPage();

        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = plugin.getConfigManager().getMainConfig().getWhoisDefaultPage();
            }
        }

        page = Math.max(1, Math.min(maxPages, page));

        WhoisProfile profile = whoisService.buildProfile(sender, target);
        sendPage(sender, profile, page, maxPages);
        return true;
    }

    private void sendPage(CommandSender sender, WhoisProfile profile, int page, int maxPages) {
        String playerName = safe(profile.playerName(), "Unknown");

        sender.sendMessage(plugin.lang().get("whois.header", "&8&m----------------------------------------"));
        sender.sendMessage(plugin.lang().format(
                "whois.title",
                "&9Whois &7» &b{player} &7(Pagina &b{page}&7/&b{max-pages}&7)",
                Map.of(
                        "player", playerName,
                        "page", String.valueOf(page),
                        "max-pages", String.valueOf(maxPages)
                )
        ));

        List<String> fields = plugin.getConfigManager().getMainConfig().getWhoisPageFields(page);
        if (fields.isEmpty()) {
            fields = getFallbackPageFields(page);
        }

        for (String field : fields) {
            if (!plugin.getConfigManager().getMainConfig().isWhoisFieldEnabled(field)) {
                continue;
            }

            String value = profile.values().get(field);
            if (value == null || value.isBlank()) {
                continue;
            }

            if ("nickname".equalsIgnoreCase(field) && !profile.hasNickname()) {
                continue;
            }

            if ("last-online".equalsIgnoreCase(field) && profile.online()) {
                continue;
            }

            String line = plugin.lang().format(
                    "whois.labels." + field,
                    "&7" + field + ": &b{value}",
                    Map.of("value", value)
            );
            sender.sendMessage(line);
        }

        sendNavigation(sender, profile.playerName(), page, maxPages);
        sender.sendMessage(plugin.lang().get("whois.footer", "&8&m----------------------------------------"));
    }

    private void sendNavigation(CommandSender sender, String playerName, int page, int maxPages) {
        String currentText = plugin.lang().format(
                "whois.navigation.current",
                "&7Pagina &b{page}&7/&b{max-pages}",
                Map.of(
                        "page", String.valueOf(page),
                        "max-pages", String.valueOf(maxPages)
                )
        );

        if (!plugin.getConfigManager().getMainConfig().isWhoisClickableNavigation() || !(sender instanceof Player player)) {
            String previous = page > 1
                    ? plugin.lang().get("whois.navigation.previous", "&b« Vorige")
                    : "&8« Vorige";

            String next = page < maxPages
                    ? plugin.lang().get("whois.navigation.next", "&bVolgende »")
                    : "&8Volgende »";

            sender.sendMessage(previous + " &8| " + currentText + " &8| " + next);
            return;
        }

        Component previous = LEGACY.deserialize(page > 1
                ? plugin.lang().get("whois.navigation.previous", "&b« Vorige")
                : "&8« Vorige");

        if (page > 1) {
            previous = previous.clickEvent(ClickEvent.runCommand("/whois " + playerName + " " + (page - 1)));
        }

        Component current = LEGACY.deserialize(currentText);

        Component next = LEGACY.deserialize(page < maxPages
                ? plugin.lang().get("whois.navigation.next", "&bVolgende »")
                : "&8Volgende »");

        if (page < maxPages) {
            next = next.clickEvent(ClickEvent.runCommand("/whois " + playerName + " " + (page + 1)));
        }

        player.sendMessage(previous
                .append(LEGACY.deserialize(" &8| "))
                .append(current)
                .append(LEGACY.deserialize(" &8| "))
                .append(next));
    }

    private List<String> getFallbackPageFields(int page) {
        if (page == 2) {
            return List.of(
                    "location",
                    "hp",
                    "hunger",
                    "exp",
                    "god",
                    "fly",
                    "speed",
                    "jailed",
                    "muted",
                    "afk",
                    "ip",
                    "alt-accounts"
            );
        }

        return List.of(
                "minecraft-name",
                "nickname",
                "mc-uuid",
                "discord-name",
                "discord-id",
                "whitelist-status",
                "gamemode",
                "op",
                "level",
                "balance",
                "playtime",
                "last-online"
        );
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    names.add(player.getName());
                }
            }
            Collections.sort(names);
            return names;
        }

        if (args.length == 2) {
            List<String> pages = new ArrayList<>();
            int maxPages = Math.max(1, plugin.getConfigManager().getMainConfig().getWhoisMaxPages());
            for (int i = 1; i <= maxPages; i++) {
                pages.add(String.valueOf(i));
            }
            return pages;
        }

        return Collections.emptyList();
    }
}