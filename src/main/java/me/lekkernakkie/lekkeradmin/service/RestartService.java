package me.lekkernakkie.lekkeradmin.service;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.MainConfig;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestartService {

    private static final Pattern DURATION_PART_PATTERN = Pattern.compile("(\\d+)([smhd])", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter TARGET_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final LekkerAdmin plugin;

    private BukkitTask tickTask;

    private ZonedDateTime scheduledAt;
    private String scheduledReason;
    private boolean manualRestart;
    private String scheduledBy;

    private final Set<Long> announcedSeconds = new HashSet<>();

    public RestartService(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduleNextAutoRestartIfNeeded();

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        clearScheduledRestart();
    }

    public boolean scheduleManualRestart(CommandSender sender, String timeInput, String reason) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        Duration duration = parseDuration(timeInput);
        if (duration == null || duration.isZero() || duration.isNegative()) {
            sender.sendMessage(color(config.getPlanRestartBadTimeMessage()));
            return false;
        }

        if (scheduledAt != null && !config.isPlanRestartAllowOverride()) {
            sender.sendMessage(color(config.getPlanRestartAlreadyPlannedMessage()));
            return false;
        }

        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        ZonedDateTime target = now.plus(duration);

        this.scheduledAt = target;
        this.scheduledReason = (reason == null || reason.isBlank()) ? config.getPlanRestartDefaultReason() : reason;
        this.manualRestart = true;
        this.scheduledBy = sender.getName();
        this.announcedSeconds.clear();

        plugin.debug("Manual restart gepland door " + sender.getName() + " om " + target + " met reden: " + this.scheduledReason);

        sender.sendMessage(color(applyPlaceholders(config.getPlanRestartPlannedMessage(), getRemainingFormatted(), this.scheduledReason, target)));
        return true;
    }

    public boolean cancelRestart(CommandSender sender) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        if (scheduledAt == null || !manualRestart) {
            sender.sendMessage(color(config.getCancelRestartNoneRunningMessage()));
            return false;
        }

        plugin.debug("Manual restart geannuleerd door " + sender.getName() + ". Vorige planning: " + scheduledAt + ", reden: " + scheduledReason);

        clearScheduledRestart();
        sender.sendMessage(color(config.getCancelRestartCancelledMessage()));

        scheduleNextAutoRestartIfNeeded();
        return true;
    }

    public boolean hasScheduledRestart() {
        return scheduledAt != null;
    }

    public boolean isManualRestart() {
        return manualRestart;
    }

    public String getScheduledReason() {
        return scheduledReason;
    }

    public String getScheduledBy() {
        return scheduledBy;
    }

    public ZonedDateTime getScheduledAt() {
        return scheduledAt;
    }

    public String getRemainingFormatted() {
        if (scheduledAt == null) {
            return "geen";
        }

        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        Duration remaining = Duration.between(now, scheduledAt);

        if (remaining.isNegative() || remaining.isZero()) {
            return "nu";
        }

        return formatDuration(remaining.getSeconds());
    }

    private void tick() {
        if (!plugin.isEnabled()) {
            return;
        }

        if (scheduledAt == null) {
            scheduleNextAutoRestartIfNeeded();
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        long remainingSeconds = Duration.between(now, scheduledAt).getSeconds();

        if (remainingSeconds <= 0) {
            executeRestart();
            return;
        }

        maybeAnnounceWarning(remainingSeconds);
    }

    private void maybeAnnounceWarning(long remainingSeconds) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        if (!config.getRestartAnnounceSeconds().contains((int) remainingSeconds)) {
            return;
        }

        if (!announcedSeconds.add(remainingSeconds)) {
            return;
        }

        String formattedTime = formatDuration(remainingSeconds);

        String chatMessage = applyPlaceholders(
                config.getRestartChatMessage(),
                formattedTime,
                scheduledReason,
                scheduledAt
        );

        Bukkit.broadcastMessage(color(chatMessage));

        if (config.isRestartTitleEnabled()) {
            String title = color(applyPlaceholders(config.getRestartTitleMain(), formattedTime, scheduledReason, scheduledAt));
            String subtitle = color(applyPlaceholders(config.getRestartTitleSub(), formattedTime, scheduledReason, scheduledAt));

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(
                        title,
                        subtitle,
                        config.getRestartTitleFadeIn(),
                        config.getRestartTitleStay(),
                        config.getRestartTitleFadeOut()
                );
            }
        }
    }

    private void executeRestart() {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        String nowMessage = applyPlaceholders(
                config.getRestartChatNowMessage(),
                "nu",
                scheduledReason,
                scheduledAt
        );

        Bukkit.broadcastMessage(color(nowMessage));

        if (config.isRestartDisconnectEnabled()) {
            String kickMessage = color(applyPlaceholders(
                    config.getRestartKickMessage(),
                    "nu",
                    scheduledReason,
                    scheduledAt
            ));

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(kickMessage);
            }
        }

        String actionType = config.getRestartActionType().trim().toLowerCase(Locale.ROOT);
        String command = config.getRestartActionCommand().trim();

        plugin.debug("Restart wordt uitgevoerd. Type=" + actionType + ", command=" + command + ", reason=" + scheduledReason);

        clearScheduledRestart();

        if ("command".equals(actionType)) {
            if (!command.isBlank()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
            }
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
    }

    private void scheduleNextAutoRestartIfNeeded() {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        if (!config.isRestartEnabled() || !config.isAutoRestartEnabled()) {
            return;
        }

        if (scheduledAt != null) {
            return;
        }

        ZonedDateTime next = findNextAutoRestart();
        if (next == null) {
            return;
        }

        this.scheduledAt = next;
        this.scheduledReason = config.getAutoRestartDefaultReason();
        this.manualRestart = false;
        this.scheduledBy = "AUTO";
        this.announcedSeconds.clear();

        plugin.debug("Volgende auto restart ingepland om " + next + " met reden: " + scheduledReason);
    }

    private ZonedDateTime findNextAutoRestart() {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        List<String> configuredTimes = config.getAutoRestartTimes();
        if (configuredTimes.isEmpty()) {
            return null;
        }

        ZoneId zoneId = getZoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        ZonedDateTime best = null;

        for (String raw : configuredTimes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            LocalTime localTime;
            try {
                localTime = LocalTime.parse(raw.trim());
            } catch (Exception ex) {
                plugin.getLogger().warning("Ongeldige restart.auto.times waarde in config.yml: " + raw);
                continue;
            }

            ZonedDateTime candidate = now.withHour(localTime.getHour())
                    .withMinute(localTime.getMinute())
                    .withSecond(0)
                    .withNano(0);

            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1);
            }

            if (best == null || candidate.isBefore(best)) {
                best = candidate;
            }
        }

        return best;
    }

    private void clearScheduledRestart() {
        this.scheduledAt = null;
        this.scheduledReason = null;
        this.manualRestart = false;
        this.scheduledBy = null;
        this.announcedSeconds.clear();
    }

    private Duration parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String normalized = input.toLowerCase(Locale.ROOT).replace(" ", "");
        Matcher matcher = DURATION_PART_PATTERN.matcher(normalized);

        long seconds = 0L;
        int matchedCharacters = 0;

        while (matcher.find()) {
            matchedCharacters += matcher.group(0).length();

            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s" -> seconds += value;
                case "m" -> seconds += value * 60L;
                case "h" -> seconds += value * 3600L;
                case "d" -> seconds += value * 86400L;
                default -> {
                    return null;
                }
            }
        }

        if (matchedCharacters != normalized.length() || seconds <= 0L) {
            return null;
        }

        return Duration.ofSeconds(seconds);
    }

    private String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "nu";
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        List<String> parts = new ArrayList<>();

        if (days > 0) {
            parts.add(days + (days == 1 ? " dag" : " dagen"));
        }

        if (hours > 0) {
            parts.add(hours + (hours == 1 ? " uur" : " uur"));
        }

        if (minutes > 0) {
            parts.add(minutes + (minutes == 1 ? " minuut" : " minuten"));
        }

        if (seconds > 0 && parts.size() < 2) {
            parts.add(seconds + (seconds == 1 ? " seconde" : " seconden"));
        }

        if (parts.isEmpty()) {
            return "nu";
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }

        return String.join(" ", parts);
    }

    private String applyPlaceholders(String message, String time, String reason, ZonedDateTime targetTime) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        String prefix = color(config.getRestartPrefix());
        String target = targetTime == null
                ? "-"
                : targetTime.format(TARGET_TIME_FORMAT) + " " + getZoneId().getId();

        return message
                .replace("{prefix}", prefix)
                .replace("{time}", time == null ? "" : time)
                .replace("{countdown}", time == null ? "" : time)
                .replace("{reason}", reason == null ? "" : reason)
                .replace("{target-time}", target)
                .replace("{date}", target);
    }

    private ZoneId getZoneId() {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        try {
            return ZoneId.of(config.getRestartTimezone());
        } catch (Exception ex) {
            plugin.getLogger().warning("Ongeldige timezone in config.yml: " + config.getRestartTimezone() + ". Fallback naar Europe/Brussels.");
            return ZoneId.of("Europe/Brussels");
        }
    }

    private String color(String text) {
        return PunishmentFormatter.colorize(text);
    }
}