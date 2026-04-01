package me.lekkernakkie.lekkeradmin.discord.embed;

import me.lekkernakkie.lekkeradmin.config.PunishmentsConfig;
import me.lekkernakkie.lekkeradmin.discord.user.DiscordPlayerInfoResolver;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.punishment.util.PunishmentFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.Instant;
import java.util.List;

public class PlayerInfoEmbedFactory {

    private final PunishmentsConfig config;

    public PlayerInfoEmbedFactory(PunishmentsConfig config) {
        this.config = config;
    }

    public MessageEmbed create(DiscordPlayerInfoResolver.PlayerInfoResult result) {
        EmbedBuilder embed = new EmbedBuilder();

        String minecraftName = fallback(result.minecraftName());
        String discordName = fallback(result.discordName());
        String discordId = fallback(result.discordId());
        String minecraftUuid = fallback(result.minecraftUuid());

        embed.setTitle("Player Info • " + minecraftName);
        embed.setColor(config.getUnmuteColor());
        embed.setTimestamp(Instant.now());

        String thumbnailUrl = config.getPlayerInfoSkinHeadUrl()
                .replace("{minecraft_name}", result.minecraftName() == null ? "Steve" : result.minecraftName());
        embed.setThumbnail(thumbnailUrl);

        embed.addField("Minecraft naam", minecraftName, true);
        embed.addField("Minecraft UUID", minecraftUuid, false);
        embed.addField("Discord naam", discordName, true);
        embed.addField("Discord ID", discordId, true);

        embed.addField("Link status", result.linked() ? "Gelinkt" : "Niet gelinkt", true);
        embed.addField("Whitelist aanvraag", result.hasApplication() ? "Ja" : "Nee", true);
        embed.addField("Aanvraag status", fallback(result.applicationStatus()), true);

        String whitelistedBy = result.whitelistedByDiscordId() == null || result.whitelistedByDiscordId().isBlank()
                ? fallback(null)
                : "<@" + result.whitelistedByDiscordId() + ">";
        embed.addField("Gewhitelist door", whitelistedBy, false);

        embed.addField("Actieve punishments", formatPunishments(result.activePunishments()), false);
        embed.addField("Verlopen / verwijderde punishments", formatPunishments(result.expiredPunishments()), false);
        embed.addField("Totaal punishments", String.valueOf(result.totalPunishments()), true);

        embed.setFooter("LekkerAdmin • Playerinfo");
        return embed.build();
    }

    private String formatPunishments(List<PunishmentEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "Geen";
        }

        StringBuilder builder = new StringBuilder();

        for (PunishmentEntry entry : entries) {
            String duration = PunishmentFormatter.formatDuration(entry.getDurationMs());
            String until = PunishmentFormatter.formatDate(
                    entry.getExpiresAt(),
                    config.getDateFormat(),
                    config.getTimezone()
            );

            builder.append("**")
                    .append(entry.getPunishmentType() == null ? "UNKNOWN" : entry.getPunishmentType().name())
                    .append("**")
                    .append(" • ")
                    .append(fallback(entry.getReason()))
                    .append("\n")
                    .append("Door: ")
                    .append(fallback(entry.getIssuedByName()))
                    .append(" | Duur: ")
                    .append(duration)
                    .append(" | Tot: ")
                    .append(until)
                    .append("\n\n");
        }

        return builder.toString().trim();
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? config.getPlayerInfoUnknownValue() : value;
    }
}