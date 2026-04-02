package me.lekkernakkie.lekkeradmin.service.whois;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.repository.PunishmentRepository;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentType;
import me.lekkernakkie.lekkeradmin.service.application.ApplicationService;
import me.lekkernakkie.lekkeradmin.service.link.LinkLookupService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WhoisService {

    private static final DecimalFormat DECIMAL = new DecimalFormat("0.##");

    private final LekkerAdmin plugin;
    private final LinkLookupService linkLookupService;
    private final ApplicationService applicationService;
    private final PunishmentRepository punishmentRepository;

    public WhoisService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.linkLookupService = new LinkLookupService(plugin);
        this.applicationService = new ApplicationService(plugin);
        this.punishmentRepository = new PunishmentRepository(plugin);
    }

    public WhoisProfile buildProfile(CommandSender viewer, OfflinePlayer target) {
        Map<String, String> values = new LinkedHashMap<>();

        Player online = target.getPlayer();
        String playerName = target.getName() == null ? "Unknown" : target.getName();
        String unknown = value("unknown", "&7Onbekend");
        String hidden = value("hidden", "&7Verborgen");
        String none = value("none", "&7Geen");

        values.put("minecraft-name", playerName);
        values.put("mc-uuid", target.getUniqueId().toString());

        String nickname = getNickname(target, online);
        boolean hasNickname = nickname != null
                && !nickname.isBlank()
                && !stripLegacy(nickname).equalsIgnoreCase(playerName);

        if (hasNickname) {
            values.put("nickname", nickname);
        }

        DiscordMinecraftLink link = linkLookupService.findByMinecraftName(playerName).orElse(null);
        values.put("discord-name", link != null && notBlank(link.getDiscordTag()) ? link.getDiscordTag() : value("not-linked", "&7Niet gekoppeld"));
        values.put("discord-id", link != null && notBlank(link.getDiscordUserId()) ? link.getDiscordUserId() : value("not-linked", "&7Niet gekoppeld"));

        values.put("whitelist-status", resolveWhitelistStatus(playerName));
        values.put("gamemode", online != null ? prettyGamemode(online.getGameMode()) : unknown);
        values.put("op", boolYesNo(target.isOp()));

        values.put("level", resolvePlaceholder(target, plugin.getConfigManager().getMainConfig().getWhoisLevelPlaceholder(), unknown));
        values.put("balance", resolvePlaceholder(target, plugin.getConfigManager().getMainConfig().getWhoisBalancePlaceholder(), unknown));
        values.put("playtime", formatPlaytime(target));
        values.put("last-online", target.isOnline() ? value("online-now", "&aNu online") : formatLastOnline(target));

        values.put("location", online != null ? formatLocation(online.getLocation()) : unknown);
        values.put("hp", online != null ? DECIMAL.format(online.getHealth()) + "/" + DECIMAL.format(online.getMaxHealth()) : unknown);
        values.put("hunger", online != null ? online.getFoodLevel() + "/20" : unknown);
        values.put("exp", online != null ? formatExp(online) : unknown);

        Object essentialsUser = getEssentialsUser(target);

        values.put("god", formatActiveInactive(readBoolean(essentialsUser, false, "isGodModeEnabled", "isGodModeEnabledRaw")));
        values.put("fly", formatActiveInactive(resolveFly(target, online, essentialsUser)));
        values.put("speed", resolveSpeed(online, essentialsUser, unknown));
        values.put("jailed", boolYesNo(readBoolean(essentialsUser, false, "isJailed")));
        values.put("afk", boolYesNo(readBoolean(essentialsUser, false, "isAfk")));

        boolean canSeeIp = viewer.hasPermission("lekkeradmin.admin") || viewer.hasPermission("lekkeradmin.whois.ip");
        String ip = resolveIp(target, online, essentialsUser);
        values.put("ip", canSeeIp ? (notBlank(ip) ? ip : unknown) : hidden);
        values.put("alt-accounts", canSeeIp ? resolveAltAccounts(target, ip, none, unknown) : hidden);

        values.put("muted", resolveMute(playerName, target));
        return new WhoisProfile(playerName, target.isOnline(), hasNickname, values);
    }

    private String resolveWhitelistStatus(String playerName) {
        WhitelistApplication application = applicationService.findByMinecraftName(playerName).orElse(null);
        if (application == null || application.getStatus() == null) {
            return value("not-whitelisted", "&cNiet gewhitelist");
        }
        return prettyEnum(application.getStatus().name());
    }

    private String resolveMute(String playerName, OfflinePlayer target) {
        PunishmentEntry mute = punishmentRepository.findActivePunishment(
                playerName,
                target.getUniqueId().toString(),
                PunishmentType.MUTE
        ).orElse(null);

        if (mute == null) {
            return boolYesNo(false);
        }

        if (mute.getExpiresAt() == null || mute.getExpiresAt() <= 0L) {
            return boolYesNo(true) + " &8(" + value("permanent", "&cPermanent") + "&8)";
        }

        long remaining = mute.getExpiresAt() - System.currentTimeMillis();
        if (remaining <= 0L) {
            return boolYesNo(false);
        }

        return boolYesNo(true) + " &8(" + formatDuration(remaining) + "&8)";
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName()
                + " "
                + location.getBlockX()
                + " "
                + location.getBlockY()
                + " "
                + location.getBlockZ();
    }

    private String formatExp(Player player) {
        return player.getLevel() + " (" + Math.round(player.getExp() * 100.0f) + "%)";
    }

    private String formatPlaytime(OfflinePlayer target) {
        try {
            Method method = target.getClass().getMethod("getStatistic", Statistic.class);
            Object result = method.invoke(target, Statistic.PLAY_ONE_MINUTE);
            if (result instanceof Integer ticks) {
                long seconds = ticks / 20L;
                return formatDuration(seconds * 1000L);
            }
        } catch (Exception ignored) {
        }
        return value("unknown", "&7Onbekend");
    }

    private String formatLastOnline(OfflinePlayer target) {
        long lastPlayed = target.getLastPlayed();
        if (lastPlayed <= 0L) {
            return value("unknown", "&7Onbekend");
        }

        long diff = System.currentTimeMillis() - lastPlayed;
        if (diff <= 0L) {
            return value("unknown", "&7Onbekend");
        }

        return formatDuration(diff) + " geleden";
    }

    private String resolvePlaceholder(OfflinePlayer target, String placeholder, String fallback) {
        if (!plugin.getConfigManager().getMainConfig().isWhoisUsePlaceholderApi()) {
            return fallback;
        }

        if (!notBlank(placeholder)) {
            return fallback;
        }

        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null || !papi.isEnabled()) {
            return fallback;
        }

        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = papiClass.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            Object result = method.invoke(null, target, placeholder);

            if (result instanceof String parsed && notBlank(parsed) && !parsed.equals(placeholder)) {
                return parsed;
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private String getNickname(OfflinePlayer target, Player online) {
        Object essentialsUser = getEssentialsUser(target);

        Object nickname = invoke(essentialsUser, "getNickname");
        if (nickname instanceof String s && notBlank(s)) {
            return s;
        }

        Object displayName = invoke(essentialsUser, "getDisplayName");
        if (displayName instanceof String s && notBlank(s)) {
            return s;
        }

        if (online != null && notBlank(online.getDisplayName()) && !online.getDisplayName().equalsIgnoreCase(online.getName())) {
            return online.getDisplayName();
        }

        return null;
    }

    private boolean resolveFly(OfflinePlayer target, Player online, Object essentialsUser) {
        Boolean essentialsFly = readBooleanNullable(essentialsUser, "isFlyModeEnabled", "getAllowFlight", "isFlying");
        if (essentialsFly != null) {
            return essentialsFly;
        }

        if (online != null) {
            return online.getAllowFlight() || online.isFlying();
        }

        return false;
    }

    private String resolveSpeed(Player online, Object essentialsUser, String fallback) {
        if (online != null) {
            float speed = online.isFlying() || online.getAllowFlight() ? online.getFlySpeed() : online.getWalkSpeed();
            return DECIMAL.format(speed);
        }

        Object walk = invoke(essentialsUser, "getWalkSpeed");
        if (walk instanceof Number n) {
            return DECIMAL.format(n.doubleValue());
        }

        Object fly = invoke(essentialsUser, "getFlySpeed");
        if (fly instanceof Number n) {
            return DECIMAL.format(n.doubleValue());
        }

        return fallback;
    }

    private String resolveIp(OfflinePlayer target, Player online, Object essentialsUser) {
        if (!plugin.getConfigManager().getMainConfig().isWhoisFieldEnabled("ip")) {
            return value("hidden", "&7Verborgen");
        }

        if (online != null && online.getAddress() != null && online.getAddress().getAddress() != null) {
            return online.getAddress().getAddress().getHostAddress();
        }

        Object address = invoke(essentialsUser, "getLastLoginAddress");
        if (address instanceof String s && notBlank(s)) {
            return cleanIp(s);
        }
        if (address instanceof InetAddress inetAddress) {
            return inetAddress.getHostAddress();
        }
        if (address instanceof InetSocketAddress socketAddress && socketAddress.getAddress() != null) {
            return socketAddress.getAddress().getHostAddress();
        }

        return null;
    }

    private String resolveAltAccounts(OfflinePlayer target, String ip, String none, String unknown) {
        if (!plugin.getConfigManager().getMainConfig().isWhoisUseEssentials()) {
            return unknown;
        }

        if (!notBlank(ip)) {
            return none;
        }

        List<String> matches = new ArrayList<>();
        for (OfflinePlayer other : Bukkit.getOfflinePlayers()) {
            if (other.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }

            String otherName = other.getName();
            if (!notBlank(otherName)) {
                continue;
            }

            Object otherUser = getEssentialsUser(other);
            String otherIp = resolveIp(other, other.getPlayer(), otherUser);
            if (notBlank(otherIp) && cleanIp(otherIp).equalsIgnoreCase(cleanIp(ip))) {
                matches.add(otherName);
            }
        }

        if (matches.isEmpty()) {
            return none;
        }

        matches.sort(String::compareToIgnoreCase);
        return String.join(", ", matches);
    }

    private Object getEssentialsUser(OfflinePlayer target) {
        if (!plugin.getConfigManager().getMainConfig().isWhoisUseEssentials()) {
            return null;
        }

        Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials == null || !essentials.isEnabled()) {
            return null;
        }

        try {
            Method getUserByUuid = essentials.getClass().getMethod("getUser", String.class);
            return getUserByUuid.invoke(essentials, target.getUniqueId().toString());
        } catch (Exception ignored) {
        }

        try {
            Method getUserByName = essentials.getClass().getMethod("getUser", String.class);
            return getUserByName.invoke(essentials, target.getName());
        } catch (Exception ignored) {
        }

        try {
            Method getOfflineUser = essentials.getClass().getMethod("getOfflineUser", String.class);
            return getOfflineUser.invoke(essentials, target.getName());
        } catch (Exception ignored) {
        }

        return null;
    }

    private String cleanIp(String ip) {
        if (ip == null) {
            return "";
        }

        String cleaned = ip.trim();
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        int slashIndex = cleaned.indexOf('/');
        if (slashIndex >= 0) {
            cleaned = cleaned.substring(0, slashIndex);
        }

        int colonCount = cleaned.length() - cleaned.replace(":", "").length();
        if (colonCount == 1 && cleaned.contains(".")) {
            cleaned = cleaned.substring(0, cleaned.lastIndexOf(':'));
        }

        return cleaned;
    }

    private String prettyGamemode(GameMode gameMode) {
        return prettyEnum(gameMode.name());
    }

    private String prettyEnum(String input) {
        String[] parts = input.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }

        return builder.toString();
    }

    private String formatActiveInactive(boolean active) {
        return active
                ? value("active", "&aActief")
                : value("inactive", "&cNiet actief");
    }

    private String boolYesNo(boolean value) {
        return value
                ? this.value("yes", "&aJa")
                : this.value("no", "&cNee");
    }

    private String value(String key, String fallback) {
        return plugin.lang().get("whois.values." + key, fallback);
    }

    private String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(Math.max(0L, millis));

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        List<String> parts = new ArrayList<>();
        if (days > 0) parts.add(days + "d");
        if (hours > 0) parts.add(hours + "u");
        if (minutes > 0) parts.add(minutes + "m");
        if (seconds > 0 || parts.isEmpty()) parts.add(seconds + "s");

        return String.join(" ", parts);
    }

    private String stripLegacy(String input) {
        return input == null ? "" : input.replaceAll("(?i)&[0-9A-FK-ORX]", "").replace('§', '&');
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Object invoke(Object instance, String methodName, Object... args) {
        if (instance == null) {
            return null;
        }

        Method[] methods = instance.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().equalsIgnoreCase(methodName)) {
                continue;
            }

            if (method.getParameterCount() != args.length) {
                continue;
            }

            try {
                return method.invoke(instance, args);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private boolean readBoolean(Object instance, boolean fallback, String... methods) {
        Boolean value = readBooleanNullable(instance, methods);
        return value != null ? value : fallback;
    }

    private Boolean readBooleanNullable(Object instance, String... methods) {
        for (String method : methods) {
            Object result = invoke(instance, method);
            if (result instanceof Boolean b) {
                return b;
            }
        }
        return null;
    }

    public record WhoisProfile(
            String playerName,
            boolean online,
            boolean hasNickname,
            Map<String, String> values
    ) {
    }
}