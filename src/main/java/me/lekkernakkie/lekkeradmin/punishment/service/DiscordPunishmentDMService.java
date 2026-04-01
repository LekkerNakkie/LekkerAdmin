package me.lekkernakkie.lekkeradmin.punishment.service;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.PunishmentsConfig;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.service.link.LinkService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DiscordPunishmentDMService {

    private final LekkerAdmin plugin;
    private final PunishmentsConfig config;
    private final LinkService linkService;

    public DiscordPunishmentDMService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getPunishmentsConfig();
        this.linkService = new LinkService(plugin);
    }

    public void sendPunishmentDM(PunishmentEntry entry) {
        if (!config.isDiscordDMEnabled() || entry == null || entry.getPunishmentType() == null) {
            return;
        }

        String typeKey = entry.getPunishmentType().name().toLowerCase();
        if (!config.isDiscordDMEnabled(typeKey)) {
            return;
        }

        String discordId = resolveDiscordId(entry.getMinecraftName());
        if (discordId == null || discordId.isBlank()) {
            return;
        }

        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) {
            return;
        }

        plugin.getDiscordManager().getJda()
                .retrieveUserById(discordId)
                .queue(user -> sendEmbed(user, entry, typeKey), error -> {});
    }

    public void sendUnbanDM(String minecraftName, String actor, String reason, boolean expired) {
        if (!config.isDiscordDMEnabled() || !config.isDiscordDMEnabled("unban")) {
            return;
        }

        String discordId = resolveDiscordId(minecraftName);
        if (discordId == null || discordId.isBlank()) {
            return;
        }

        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) {
            return;
        }

        plugin.getDiscordManager().getJda()
                .retrieveUserById(discordId)
                .queue(user -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(config.getDiscordTitle("unban"));
                    embed.setColor(config.getDiscordDmColor("unban"));
                    embed.setTimestamp(Instant.now());
                    embed.addField("Speler", fallback(minecraftName), true);
                    embed.addField("Door", expired ? "Automatisch" : fallback(actor), true);
                    embed.addField("Reden", expired ? "Ban verlopen" : fallback(reason), false);

                    user.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(embed.build()).queue(), error -> {});
                }, error -> {});
    }

    public void sendUnmuteDM(String minecraftName, String actor, String reason) {
        if (!config.isDiscordDMEnabled() || !config.isDiscordDMEnabled("unmute")) {
            return;
        }

        String discordId = resolveDiscordId(minecraftName);
        if (discordId == null || discordId.isBlank()) {
            return;
        }

        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) {
            return;
        }

        plugin.getDiscordManager().getJda()
                .retrieveUserById(discordId)
                .queue(user -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(config.getDiscordTitle("unmute"));
                    embed.setColor(config.getDiscordDmColor("unmute"));
                    embed.setTimestamp(Instant.now());
                    embed.addField("Speler", fallback(minecraftName), true);
                    embed.addField("Door", fallback(actor), true);
                    embed.addField("Reden", fallback(reason), false);

                    user.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(embed.build()).queue(), error -> {});
                }, error -> {});
    }

    private void sendEmbed(User user, PunishmentEntry entry, String typeKey) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(config.getDiscordTitle(typeKey));
        embed.setColor(config.getDiscordDmColor(typeKey));
        embed.setTimestamp(Instant.now());

        embed.addField("Speler", fallback(entry.getMinecraftName()), true);
        embed.addField("Door", fallback(entry.getIssuedByName()), true);
        embed.addField("Reden", fallback(entry.getReason()), false);
        embed.addField("Wanneer", formatDate(entry.getIssuedAt()), true);

        if (entry.getDurationMs() != null) {
            embed.addField("Duur", formatDuration(entry.getDurationMs()), true);
        }

        if (entry.getExpiresAt() != null) {
            embed.addField("Tot", formatDate(entry.getExpiresAt()), true);
        }

        user.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(embed.build()).queue(), error -> {});
    }

    private String resolveDiscordId(String minecraftName) {
        if (minecraftName == null || minecraftName.isBlank()) {
            return null;
        }

        Optional<DiscordMinecraftLink> link = linkService.findByMinecraftName(minecraftName);
        return link.map(DiscordMinecraftLink::getDiscordUserId).orElse(null);
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "Onbekend" : value;
    }

    private String formatDate(long timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(config.getDateFormat())
                .withZone(ZoneId.of(config.getTimezone()));
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