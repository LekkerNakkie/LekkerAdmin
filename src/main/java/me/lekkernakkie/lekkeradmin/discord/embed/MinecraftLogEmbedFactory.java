package me.lekkernakkie.lekkeradmin.discord.embed;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogEmbedConfig;
import me.lekkernakkie.lekkeradmin.config.logs.LogFieldConfig;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.discord.log.MinecraftLogMessage;
import me.lekkernakkie.lekkeradmin.discord.util.DiscordColorUtil;
import me.lekkernakkie.lekkeradmin.discord.util.DiscordPlaceholderUtil;
import me.lekkernakkie.lekkeradmin.model.log.ExplosionLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerDeathLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerDropLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerJoinLeaveLogContext;
import me.lekkernakkie.lekkeradmin.model.log.PlayerPickupLogContext;
import me.lekkernakkie.lekkeradmin.util.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinecraftLogEmbedFactory {

    private static final int DISCORD_FIELD_LIMIT = 1024;

    private final LekkerAdmin plugin;

    public MinecraftLogEmbedFactory(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public MinecraftLogMessage createDropMessage(LogTypeSettings settings, PlayerDropLogContext context) {
        LogEmbedConfig embedConfig = settings.getEmbedConfig();
        LogFieldConfig fields = embedConfig.getFields();

        String items = context.getCustomSummary() != null && !context.getCustomSummary().isBlank()
                ? context.getCustomSummary()
                : context.getItemSummary();

        Map<String, String> placeholders = basePlaceholders(context.getPlayerName(), context.getWorldName(), context.getCoordinates());
        placeholders.put("item", items);
        placeholders.put("amount", String.valueOf(context.getAmount()));

        EmbedBuilder embed = baseEmbed(embedConfig, placeholders, context.getPlayerName());

        embed.addField(fields.getPlayer(), safe(context.getPlayerName()), true);

        if (embedConfig.isShowWorld()) {
            embed.addField(fields.getWorld(), safe(context.getWorldName()), true);
        }

        if (embedConfig.isShowCoordinates()) {
            embed.addField(fields.getCoordinates(), safe(context.getCoordinates()), true);
        }

        addSplitField(embed, fields.getItem(), items, embedConfig.getMaxFieldLength());

        if (context.getDestroyedItemsSummary() != null && !context.getDestroyedItemsSummary().isBlank()) {
            addSplitField(embed, "Vernietigd", context.getDestroyedItemsSummary(), embedConfig.getMaxFieldLength());
        }

        if (embedConfig.isShowEnchantments() && context.hasEnchantments()) {
            addSplitField(embed, fields.getEnchantments(), context.getEnchantmentsSummary(), embedConfig.getMaxFieldLength());
        }

        return new MinecraftLogMessage("", embed.build(), new ArrayList<>());
    }

    public MinecraftLogMessage createPickupMessage(LogTypeSettings settings, PlayerPickupLogContext context) {
        LogEmbedConfig embedConfig = settings.getEmbedConfig();
        LogFieldConfig fields = embedConfig.getFields();

        String items = context.getCustomSummary() != null && !context.getCustomSummary().isBlank()
                ? context.getCustomSummary()
                : context.getItemSummary();

        Map<String, String> placeholders = basePlaceholders(context.getPlayerName(), context.getWorldName(), context.getCoordinates());
        placeholders.put("item", items);
        placeholders.put("amount", String.valueOf(context.getAmount()));

        EmbedBuilder embed = baseEmbed(embedConfig, placeholders, context.getPlayerName());

        embed.addField(fields.getPlayer(), safe(context.getPlayerName()), true);

        if (embedConfig.isShowWorld()) {
            embed.addField(fields.getWorld(), safe(context.getWorldName()), true);
        }

        if (embedConfig.isShowCoordinates()) {
            embed.addField(fields.getCoordinates(), safe(context.getCoordinates()), true);
        }

        addSplitField(embed, fields.getItem(), items, embedConfig.getMaxFieldLength());

        if (embedConfig.isShowEnchantments() && context.hasEnchantments()) {
            addSplitField(embed, fields.getEnchantments(), context.getEnchantmentsSummary(), embedConfig.getMaxFieldLength());
        }

        return new MinecraftLogMessage("", embed.build(), new ArrayList<>());
    }

    public MinecraftLogMessage createDeathMessage(LogTypeSettings settings, PlayerDeathLogContext context) {
        LogEmbedConfig embedConfig = settings.getEmbedConfig();
        LogFieldConfig fields = embedConfig.getFields();

        Map<String, String> placeholders = basePlaceholders(context.getPlayerName(), context.getWorldName(), context.getCoordinates());
        placeholders.put("cause", context.getCause());
        placeholders.put("killer", context.getKillerName());

        EmbedBuilder embed = baseEmbed(embedConfig, placeholders, context.getPlayerName());

        embed.addField(fields.getPlayer(), safe(context.getPlayerName()), true);

        if (embedConfig.isShowWorld()) {
            embed.addField(fields.getWorld(), safe(context.getWorldName()), true);
        }

        if (embedConfig.isShowCoordinates()) {
            embed.addField(fields.getCoordinates(), safe(context.getCoordinates()), true);
        }

        embed.addField(fields.getCause(), safe(context.getCause()), true);
        embed.addField(fields.getKiller(), safe(context.getKillerName()), true);
        embed.addField(fields.getXp(), safe(context.getXpSummary()), true);
        embed.addField(fields.getKeepInventory(), context.isKeepInventory() ? "Ja" : "Nee", true);

        String dropSummary = context.getDroppedItemsSummary();
        if (dropSummary != null && !dropSummary.isBlank()) {
            addSplitField(embed, fields.getDroppedItems(), dropSummary, embedConfig.getMaxFieldLength());
        }

        if (context.getDestroyedItemsSummary() != null && !context.getDestroyedItemsSummary().isBlank()) {
            addSplitField(embed, "Vernietigd", context.getDestroyedItemsSummary(), embedConfig.getMaxFieldLength());
        }

        return new MinecraftLogMessage("", embed.build(), new ArrayList<>());
    }

    public MinecraftLogMessage createJoinMessage(LogTypeSettings settings, PlayerJoinLeaveLogContext context) {
        LogEmbedConfig embedConfig = settings.getEmbedConfig();
        LogFieldConfig fields = embedConfig.getFields();

        Map<String, String> placeholders = basePlaceholders(context.getPlayerName(), context.getWorldName(), context.getCoordinates());
        placeholders.put("reason", context.getReason());
        placeholders.put("health", context.getHealth());
        placeholders.put("food", String.valueOf(context.getFood()));
        placeholders.put("gamemode", context.getGameMode());

        EmbedBuilder embed = baseEmbed(embedConfig, placeholders, context.getPlayerName());

        embed.addField(fields.getPlayer(), safe(context.getPlayerName()), true);
        embed.addField(fields.getReason(), safe(context.getReason()), true);

        if (embedConfig.isShowWorld()) {
            embed.addField(fields.getWorld(), safe(context.getWorldName()), true);
        }

        if (embedConfig.isShowCoordinates()) {
            embed.addField(fields.getCoordinates(), safe(context.getCoordinates()), true);
        }

        embed.addField(fields.getHealth(), safe(context.getHealth()), true);
        embed.addField(fields.getFood(), String.valueOf(context.getFood()), true);
        embed.addField(fields.getGamemode(), safe(context.getGameMode()), true);

        return new MinecraftLogMessage("", embed.build(), new ArrayList<>());
    }

    public MinecraftLogMessage createLeaveMessage(LogTypeSettings settings, PlayerJoinLeaveLogContext context) {
        return createJoinMessage(settings, context);
    }

    public MinecraftLogMessage createExplosionMessage(LogTypeSettings settings, ExplosionLogContext context) {
        LogEmbedConfig embedConfig = settings.getEmbedConfig();

        Map<String, String> placeholders = basePlaceholders(context.getTriggeredBy(), context.getWorldName(), context.getCoordinates());
        placeholders.put("type", safe(context.getType()));
        placeholders.put("triggered_by", safe(context.getTriggeredBy()));
        placeholders.put("region", safe(context.getRegion()));
        placeholders.put("destroyed_blocks", String.valueOf(context.getDestroyedBlockCount()));
        placeholders.put("chain", String.valueOf(context.getChainSize()));
        placeholders.put("cancelled", context.isCancelled() ? "Ja" : "Nee");

        EmbedBuilder embed = baseEmbed(embedConfig, placeholders, context.getTriggeredBy());

        embed.addField("Type", safe(context.getType()), true);
        embed.addField("Triggered by", safe(context.getTriggeredBy()), true);

        if (embedConfig.isShowWorld()) {
            embed.addField("Wereld", safe(context.getWorldName()), true);
        }

        if (embedConfig.isShowCoordinates()) {
            embed.addField("Coördinaten", safe(context.getCoordinates()), true);
        }

        embed.addField("Region", safe(context.getRegion()), true);
        embed.addField("Cancelled", context.isCancelled() ? "Ja" : "Nee", true);
        embed.addField("Blocks geraakt", String.valueOf(context.getDestroyedBlockCount()), true);
        embed.addField("Chain", String.valueOf(context.getChainSize()), true);

        if (context.getDestroyedBlocksSummary() != null && !context.getDestroyedBlocksSummary().isBlank()) {
            addSplitField(embed, "Blocks", context.getDestroyedBlocksSummary(), embedConfig.getMaxFieldLength());
        }

        if (context.getContainersSummary() != null && !context.getContainersSummary().isBlank()) {
            addSplitField(embed, "Containers Hit", context.getContainersSummary(), embedConfig.getMaxFieldLength());
        }

        if (context.getAlertSummary() != null && !context.getAlertSummary().isBlank()) {
            addSplitField(embed, "Alert", context.getAlertSummary(), embedConfig.getMaxFieldLength());
        }

        return new MinecraftLogMessage("", embed.build(), new ArrayList<>());
    }

    private EmbedBuilder baseEmbed(LogEmbedConfig config, Map<String, String> placeholders, String playerName) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(DiscordPlaceholderUtil.apply(config.getTitle(), placeholders));
        embed.setDescription(DiscordPlaceholderUtil.apply(config.getDescription(), placeholders));
        embed.setColor(DiscordColorUtil.fromHex(config.getColor(), Color.decode("#5865F2")));

        if (config.getFooter() != null && !config.getFooter().isBlank()) {
            embed.setFooter(DiscordPlaceholderUtil.apply(config.getFooter(), placeholders));
        }

        if (config.isUseTimestamp()) {
            embed.setTimestamp(Instant.now());
        }

        if (config.isShowPlayerHeadThumbnail() && playerName != null && !playerName.isBlank() && !playerName.equals("-")) {
            embed.setThumbnail("https://minotar.net/avatar/" + playerName + "/64");
        }

        return embed;
    }

    private Map<String, String> basePlaceholders(String player, String world, String coordinates) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", safe(player));
        placeholders.put("world", safe(world));
        placeholders.put("coordinates", safe(coordinates));
        return placeholders;
    }

    private void addSplitField(EmbedBuilder embed, String title, String content, int configuredMaxLength) {
        String safeContent = safe(content);
        if (safeContent.isBlank()) {
            return;
        }

        int maxLength = configuredMaxLength > 0
                ? Math.min(configuredMaxLength, DISCORD_FIELD_LIMIT)
                : DISCORD_FIELD_LIMIT;

        List<String> chunks = splitForDiscordField(safeContent, maxLength);

        for (int i = 0; i < chunks.size(); i++) {
            String fieldName = (i == 0) ? title : title + " (" + (i + 1) + ")";
            embed.addField(fieldName, chunks.get(i), false);
        }
    }

    private List<String> splitForDiscordField(String input, int maxLength) {
        List<String> result = new ArrayList<>();
        String[] lines = input.replace("\r", "").split("\n");
        StringBuilder current = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine;

            if (line.length() > maxLength) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }

                int start = 0;
                while (start < line.length()) {
                    int end = Math.min(start + maxLength, line.length());
                    result.add(line.substring(start, end));
                    start = end;
                }
                continue;
            }

            int additionalLength = current.length() == 0 ? line.length() : 1 + line.length();
            if (current.length() + additionalLength > maxLength) {
                result.add(current.toString());
                current.setLength(0);
            }

            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(line);
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        if (result.isEmpty()) {
            result.add("-");
        }

        return result;
    }

    private String safe(String input) {
        return StringUtil.safe(input);
    }
}