package me.lekkernakkie.lekkeradmin.service;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private final Set<ZonedDateTime> skippedAutoRestarts = new HashSet<>();

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
        skippedAutoRestarts.clear();
    }

    public boolean scheduleManualRestart(CommandSender sender, String timeInput, String reason) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        Duration duration = parseDuration(timeInput);
        if (duration == null || duration.isZero() || duration.isNegative()) {
            sender.sendMessage(plugin.lang().message(
                    "restart.invalid-time",
                    "&cOngeldige tijd&7. Gebruik bv: &b10s&7, &b5m&7, &b2h&7, &b1h30m"
            ));
            return false;
        }

        if (scheduledAt != null && !config.isPlanRestartAllowOverride()) {
            sender.sendMessage(plugin.lang().message(
                    "restart.already-planned",
                    "&cEr is al een restart gepland en overschrijven is uitgeschakeld."
            ));
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

        sender.sendMessage(plugin.lang().format(
                "restart.planned",
                "{prefix}&7Restart gepland over &b{time}&7. Reden: &b{reason}",
                Map.of(
                        "time", getRemainingFormatted(),
                        "reason", this.scheduledReason
                )
        ));
        return true;
    }

    private boolean cancelManualRestart(CommandSender sender) {
        if (scheduledAt == null || !manualRestart) {
            sender.sendMessage(plugin.lang().message(
                    "restart.none-running",
                    "&cEr is momenteel geen restart gepland."
            ));
            return false;
        }

        plugin.debug("Manual restart geannuleerd door " + sender.getName() + ". Vorige planning: " + scheduledAt + ", reden: " + scheduledReason);

        clearScheduledRestart();
        sender.sendMessage(plugin.lang().message(
                "restart.cancelled",
                "{prefix}&7De geplande restart is &cgeannuleerd&7."
        ));

        scheduleNextAutoRestartIfNeeded();
        return true;
    }

    public boolean cancelUpcomingRestart(CommandSender sender, int index) {
        if (index <= 0) {
            sender.sendMessage(plugin.lang().message(
                    "restart.cancel-invalid-index",
                    "{prefix}&cOngeldige restart index. Gebruik bv: &b/cancelrestart 2"
            ));
            return false;
        }

        List<UpcomingRestart> upcoming = getUpcomingRestarts(Math.max(3, index));
        if (upcoming.size() < index) {
            sender.sendMessage(plugin.lang().format(
                    "restart.cancel-index-not-found",
                    "{prefix}&cEr is geen komende restart met index &b{index}&c.",
                    Map.of("index", String.valueOf(index))
            ));
            return false;
        }

        UpcomingRestart selected = upcoming.get(index - 1);

        if (selected.manual()) {
            if (scheduledAt != null && manualRestart && scheduledAt.equals(selected.time())) {
                return cancelManualRestart(sender);
            }

            sender.sendMessage(plugin.lang().format(
                    "restart.cancel-index-not-found",
                    "{prefix}&cEr is geen komende restart met index &b{index}&c.",
                    Map.of("index", String.valueOf(index))
            ));
            return false;
        }

        skippedAutoRestarts.add(selected.time());

        if (scheduledAt != null && !manualRestart && scheduledAt.equals(selected.time())) {
            clearScheduledRestart();
            scheduleNextAutoRestartIfNeeded();
        }

        sender.sendMessage(plugin.lang().format(
                "restart.cancelled-auto-once",
                "{prefix}&7Automatische restart &b#{index} &7werd eenmalig &cgeskipt&7.",
                Map.of(
                        "index", String.valueOf(index),
                        "target-time", formatTargetTime(selected.time())
                )
        ));

        plugin.debug("Auto restart occurrence skipped door " + sender.getName() + ": " + selected.time());
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

    public String formatDurationUntil(ZonedDateTime target) {
        if (target == null) {
            return "-";
        }

        long seconds = Duration.between(ZonedDateTime.now(getZoneId()), target).getSeconds();
        if (seconds <= 0) {
            return "nu";
        }

        return formatDuration(seconds);
    }

    public String formatTargetTime(ZonedDateTime targetTime) {
        if (targetTime == null) {
            return "-";
        }
        return targetTime.format(TARGET_TIME_FORMAT);
    }

    public List<UpcomingRestart> getUpcomingRestarts(int limit) {
        List<UpcomingRestart> result = new ArrayList<>();
        ZoneId zoneId = getZoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        pruneSkippedAutoRestarts(now);

        if (scheduledAt != null) {
            result.add(new UpcomingRestart(scheduledAt, scheduledReason, manualRestart, scheduledBy));
        }

        List<ZonedDateTime> autoCandidates = findNextAutoRestarts(limit + 5);
        for (ZonedDateTime autoTime : autoCandidates) {
            if (scheduledAt != null && scheduledAt.equals(autoTime)) {
                continue;
            }

            result.add(new UpcomingRestart(
                    autoTime,
                    plugin.getConfigManager().getMainConfig().getAutoRestartDefaultReason(),
                    false,
                    "AUTO"
            ));
        }

        result.sort(Comparator.comparing(UpcomingRestart::time));

        if (result.size() > limit) {
            return new ArrayList<>(result.subList(0, limit));
        }

        return result;
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

        String chatMessage = plugin.lang().format(
                "restart.chat",
                "{restart-prefix}&7Server restart over &b{time}&7. Reden: &b{reason}",
                Map.of(
                        "time", formattedTime,
                        "reason", scheduledReason == null ? "" : scheduledReason
                )
        );

        Bukkit.broadcastMessage(chatMessage);

        if (config.isRestartTitleEnabled()) {
            String title = plugin.lang().format(
                    "restart.title-main",
                    "&7Restart over &b{time}",
                    Map.of("time", formattedTime)
            );

            String subtitle = plugin.lang().format(
                    "restart.title-sub",
                    "&7Reden: &b{reason}",
                    Map.of("reason", scheduledReason == null ? "" : scheduledReason)
            );

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

        String nowMessage = plugin.lang().format(
                "restart.chat-now",
                "{restart-prefix}&cServer restart nu&7. Reden: &b{reason}",
                Map.of(
                        "reason", scheduledReason == null ? "" : scheduledReason
                )
        );

        Bukkit.broadcastMessage(nowMessage);

        if (config.isRestartDisconnectEnabled()) {
            String kickMessage = plugin.lang().format(
                    "restart.kick-message",
                    "&cServer restart!\n\n&7Reden: &b{reason}\n&7Je kan direct terug joinen.",
                    Map.of("reason", scheduledReason == null ? "" : scheduledReason)
            );

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(kickMessage);
            }
        }

        String actionType = config.getRestartActionType().trim().toLowerCase(Locale.ROOT);
        String command = config.getRestartActionCommand().trim();

        plugin.debug("Restart wordt uitgevoerd. Type=" + actionType + ", command=" + command + ", reason=" + scheduledReason);

        ZonedDateTime executedAt = scheduledAt;
        boolean executedManual = manualRestart;

        clearScheduledRestart();

        if (!executedManual && executedAt != null) {
            skippedAutoRestarts.remove(executedAt);
        }

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
        List<ZonedDateTime> candidates = findNextAutoRestarts(1);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private List<ZonedDateTime> findNextAutoRestarts(int amount) {
        MainConfig config = plugin.getConfigManager().getMainConfig();
        List<String> configuredTimes = config.getAutoRestartTimes();

        if (configuredTimes.isEmpty() || amount <= 0) {
            return List.of();
        }

        ZoneId zoneId = getZoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        pruneSkippedAutoRestarts(now);

        List<LocalTime> localTimes = new ArrayList<>();
        for (String raw : configuredTimes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            try {
                localTimes.add(LocalTime.parse(raw.trim()));
            } catch (Exception ex) {
                plugin.getLogger().warning("Ongeldige restart.auto.times waarde in config.yml: " + raw);
            }
        }

        localTimes.sort(Comparator.naturalOrder());

        List<ZonedDateTime> results = new ArrayList<>();
        int daysChecked = 0;

        while (results.size() < amount && daysChecked < 30) {
            ZonedDateTime dayBase = now.plusDays(daysChecked);

            for (LocalTime localTime : localTimes) {
                ZonedDateTime candidate = dayBase.withHour(localTime.getHour())
                        .withMinute(localTime.getMinute())
                        .withSecond(0)
                        .withNano(0);

                if (!candidate.isAfter(now)) {
                    continue;
                }

                if (skippedAutoRestarts.contains(candidate)) {
                    continue;
                }

                results.add(candidate);
                if (results.size() >= amount) {
                    break;
                }
            }

            daysChecked++;
        }

        return results;
    }

    private void pruneSkippedAutoRestarts(ZonedDateTime now) {
        skippedAutoRestarts.removeIf(time -> !time.isAfter(now));
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

    public String formatDuration(long totalSeconds) {
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
            parts.add(hours + " uur");
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

    private ZoneId getZoneId() {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        try {
            return ZoneId.of(config.getRestartTimezone());
        } catch (Exception ex) {
            plugin.getLogger().warning("Ongeldige timezone in config.yml: " + config.getRestartTimezone() + ". Fallback naar Europe/Brussels.");
            return ZoneId.of("Europe/Brussels");
        }
    }

    public record UpcomingRestart(
            ZonedDateTime time,
            String reason,
            boolean manual,
            String scheduledBy
    ) {
    }
}