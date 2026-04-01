package me.lekkernakkie.lekkeradmin.discord.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordPunishmentLogger {

    private final LekkerAdmin plugin;
    private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean draining = new AtomicBoolean(false);

    public DiscordPunishmentLogger(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void logBan(PunishmentEntry entry) {
        sendEntryLog("BAN", entry);
    }

    public void logUnban(PunishmentEntry entry, String actor, String reason) {
        sendManualLog("UNBAN", entry, actor, reason);
    }

    public void logMute(PunishmentEntry entry) {
        sendEntryLog("MUTE", entry);
    }

    public void logUnmute(PunishmentEntry entry, String actor, String reason) {
        sendManualLog("UNMUTE", entry, actor, reason);
    }

    public void logKick(PunishmentEntry entry) {
        sendEntryLog("KICK", entry);
    }

    public void logWarn(PunishmentEntry entry) {
        sendEntryLog("WARN", entry);
    }

    public void logHistoryClearSingle(String actor, String target, PunishmentEntry entry, String clearReason) {
        LogTypeSettings settings = getHistoryClearSettings();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        String channelId = settings.getChannelConfig().getChannelId();
        if (channelId == null || channelId.isBlank()) {
            return;
        }

        enqueue(() -> {
            TextChannel channel = resolveChannel(channelId);
            if (channel == null) {
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(resolveTitle(settings, "CLEARHISTORY"));

            String description = resolveDescription(settings, "CLEARHISTORY", actor, target, clearReason);
            if (description != null && !description.isBlank()) {
                embed.setDescription(description);
            }

            embed.setColor(resolveHistoryClearColor(settings));
            embed.setTimestamp(Instant.now());

            if (settings.getEmbedConfig().getFooter() != null && !settings.getEmbedConfig().getFooter().isBlank()) {
                embed.setFooter(settings.getEmbedConfig().getFooter());
            }

            embed.addField("Uitgevoerd door", fallback(actor), true);
            embed.addField("Speler", fallback(target), true);
            embed.addField("Actie", "SINGLE", true);
            embed.addField("Punishment ID", entry == null ? "-" : String.valueOf(entry.getId()), true);
            embed.addField("Sanctie", entry == null || entry.getPunishmentType() == null ? "-" : entry.getPunishmentType().name(), true);
            embed.addField("Originele reden", entry == null ? "-" : fallback(entry.getReason()), false);
            embed.addField("Clearhistory reden", fallback(clearReason), false);

            send(channel, embed);
        });
    }

    public void logHistoryClearAll(String actor, String target, int amount, String clearReason) {
        LogTypeSettings settings = getHistoryClearSettings();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        String channelId = settings.getChannelConfig().getChannelId();
        if (channelId == null || channelId.isBlank()) {
            return;
        }

        enqueue(() -> {
            TextChannel channel = resolveChannel(channelId);
            if (channel == null) {
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(resolveTitle(settings, "CLEARHISTORY"));

            String description = resolveDescription(settings, "CLEARHISTORY", actor, target, clearReason);
            if (description != null && !description.isBlank()) {
                embed.setDescription(description);
            }

            embed.setColor(resolveHistoryClearColor(settings));
            embed.setTimestamp(Instant.now());

            if (settings.getEmbedConfig().getFooter() != null && !settings.getEmbedConfig().getFooter().isBlank()) {
                embed.setFooter(settings.getEmbedConfig().getFooter());
            }

            embed.addField("Uitgevoerd door", fallback(actor), true);
            embed.addField("Speler", fallback(target), true);
            embed.addField("Actie", "ALL", true);
            embed.addField("Aantal", String.valueOf(amount), true);
            embed.addField("Clearhistory reden", fallback(clearReason), false);

            send(channel, embed);
        });
    }

    private void sendEntryLog(String action, PunishmentEntry entry) {
        LogTypeSettings settings = getPunishmentSettings();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        String channelId = settings.getChannelConfig().getChannelId();
        if (channelId == null || channelId.isBlank()) {
            return;
        }

        enqueue(() -> {
            TextChannel channel = resolveChannel(channelId);
            if (channel == null) {
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(resolveTitle(settings, action));
            String description = resolveDescription(settings, action, entry.getIssuedByName(), entry.getMinecraftName(), entry.getReason());
            if (description != null && !description.isBlank()) {
                embed.setDescription(description);
            }
            embed.setColor(resolveColor(settings, action));
            embed.setTimestamp(Instant.now());

            if (settings.getEmbedConfig().getFooter() != null && !settings.getEmbedConfig().getFooter().isBlank()) {
                embed.setFooter(settings.getEmbedConfig().getFooter());
            }

            embed.addField("Speler", fallback(entry.getMinecraftName()), true);
            embed.addField("Door", fallback(entry.getIssuedByName()), true);
            embed.addField("Type", fallback(action), true);
            embed.addField("Reden", fallback(entry.getReason()), false);
            embed.addField("Wanneer", formatDate(entry.getIssuedAt()), true);

            if (entry.getDurationMs() != null) {
                embed.addField("Duur", formatDuration(entry.getDurationMs()), true);
            }

            if (entry.getExpiresAt() != null) {
                embed.addField("Tot", formatDate(entry.getExpiresAt()), true);
            }

            send(channel, embed);
        });
    }

    private void sendManualLog(String action, PunishmentEntry entry, String actor, String reason) {
        LogTypeSettings settings = getPunishmentSettings();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        String channelId = settings.getChannelConfig().getChannelId();
        if (channelId == null || channelId.isBlank()) {
            return;
        }

        enqueue(() -> {
            TextChannel channel = resolveChannel(channelId);
            if (channel == null) {
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(resolveTitle(settings, action));
            String description = resolveDescription(settings, action, actor, entry.getMinecraftName(), reason);
            if (description != null && !description.isBlank()) {
                embed.setDescription(description);
            }
            embed.setColor(resolveColor(settings, action));
            embed.setTimestamp(Instant.now());

            if (settings.getEmbedConfig().getFooter() != null && !settings.getEmbedConfig().getFooter().isBlank()) {
                embed.setFooter(settings.getEmbedConfig().getFooter());
            }

            embed.addField("Speler", fallback(entry.getMinecraftName()), true);
            embed.addField("Door", fallback(actor), true);
            embed.addField("Type", fallback(action), true);
            embed.addField("Reden", fallback(reason), false);

            send(channel, embed);
        });
    }

    private LogTypeSettings getPunishmentSettings() {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return null;
        }
        return plugin.getConfigManager().getLogsConfig().getPunishmentLogs();
    }

    private LogTypeSettings getHistoryClearSettings() {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return null;
        }
        return plugin.getConfigManager().getLogsConfig().getHistoryClearLogs();
    }

    private TextChannel resolveChannel(String channelId) {
        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) {
            return null;
        }
        return plugin.getDiscordManager().getJda().getTextChannelById(channelId);
    }

    private void send(TextChannel channel, EmbedBuilder embed) {
        channel.sendMessageEmbeds(embed.build()).queue(
                success -> { },
                error -> plugin.debug("Discord punishment log send failed: " + error.getMessage())
        );
    }

    private void enqueue(Runnable action) {
        queue.add(action);
        drain();
    }

    private void drain() {
        if (!draining.compareAndSet(false, true)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task -> {
            Runnable next = queue.poll();
            if (next == null) {
                draining.set(false);
                task.cancel();
                return;
            }
            try {
                next.run();
            } catch (Exception ex) {
                plugin.debug("Discord punishment log queue error: " + ex.getMessage());
            }
        }, 0L, 10L);
    }

    private String resolveTitle(LogTypeSettings settings, String action) {
        String title = settings.getEmbedConfig().getTitle();
        if (title == null || title.isBlank()) {
            return action;
        }
        return title.replace("{action}", action);
    }

    private String resolveDescription(LogTypeSettings settings, String action, String actor, String player, String reason) {
        String description = settings.getEmbedConfig().getDescription();
        if (description == null || description.isBlank()) {
            return null;
        }

        return description
                .replace("{action}", fallback(action))
                .replace("{actor}", fallback(actor))
                .replace("{player}", fallback(player))
                .replace("{reason}", fallback(reason));
    }

    private Color resolveColor(LogTypeSettings settings, String action) {
        String configured = settings.getEmbedConfig().getColor();
        String fallback = switch (action.toUpperCase()) {
            case "BAN" -> "#FF4D4D";
            case "UNBAN" -> "#33CC66";
            case "MUTE" -> "#FFB347";
            case "UNMUTE" -> "#66CCFF";
            case "KICK" -> "#F39C12";
            case "WARN" -> "#F1C40F";
            default -> "#5865F2";
        };

        try {
            return Color.decode(configured == null || configured.isBlank() ? fallback : configured);
        } catch (Exception ex) {
            return Color.decode(fallback);
        }
    }

    private Color resolveHistoryClearColor(LogTypeSettings settings) {
        String configured = settings.getEmbedConfig().getColor();
        String fallback = "#95A5A6";

        try {
            return Color.decode(configured == null || configured.isBlank() ? fallback : configured);
        } catch (Exception ex) {
            return Color.decode(fallback);
        }
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "Onbekend" : value;
    }

    private String formatDate(long timestamp) {
        String timezone = "Europe/Brussels";
        if (plugin.getConfigManager() != null && plugin.getConfigManager().getLogsConfig() != null) {
            timezone = plugin.getConfigManager().getLogsConfig().getTimezone();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                .withZone(ZoneId.of(timezone));
        return formatter.format(Instant.ofEpochMilli(timestamp));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000L;
        long weeks = seconds / 604800L;
        seconds %= 604800L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        StringBuilder builder = new StringBuilder();
        if (weeks > 0) builder.append(weeks).append("w ");
        if (days > 0) builder.append(days).append("d ");
        if (hours > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");
        if (seconds > 0 && builder.isEmpty()) builder.append(seconds).append("s");

        String result = builder.toString().trim();
        return result.isBlank() ? "Permanent" : result;
    }
}