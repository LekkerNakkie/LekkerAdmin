package me.lekkernakkie.lekkeradmin.util.log;

import me.lekkernakkie.lekkeradmin.model.log.LoggedItemData;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemLogUtil {

    private ItemLogUtil() {
    }

    public static LoggedItemData fromItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return new LoggedItemData(
                    "AIR",
                    0,
                    "-",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    false,
                    "",
                    null
            );
        }

        ItemMeta meta = itemStack.getItemMeta();

        String material = itemStack.getType().name();
        int amount = itemStack.getAmount();
        String displayName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : material;

        List<String> enchantments = new ArrayList<>();
        if (meta != null) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchantments.add(entry.getKey().getKey().getKey() + " " + entry.getValue());
            }
        }

        List<String> lore = new ArrayList<>();
        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            lore.addAll(meta.getLore());
        }

        boolean enchanted = meta != null && !meta.getEnchants().isEmpty();
        String skullInfo = resolveSkullInfo(itemStack, meta);
        Integer customModelData = resolveCustomModelData(meta);

        return new LoggedItemData(
                material,
                amount,
                displayName,
                enchantments,
                lore,
                enchanted,
                skullInfo,
                customModelData
        );
    }

    public static String formatSingleLine(LoggedItemData itemData) {
        if (itemData == null) {
            return "-";
        }

        StringBuilder line = new StringBuilder();

        if (itemData.getDisplayName() != null
                && !itemData.getDisplayName().isBlank()
                && !itemData.getDisplayName().equalsIgnoreCase(itemData.getMaterial())) {
            line.append(itemData.getDisplayName())
                    .append(" (")
                    .append(itemData.getMaterial())
                    .append(") x")
                    .append(itemData.getAmount());
        } else {
            line.append(itemData.getMaterial())
                    .append(" x")
                    .append(itemData.getAmount());
        }

        appendExtraInfo(line, itemData);
        return line.toString();
    }

    public static String formatEnchantments(LoggedItemData itemData) {
        if (itemData == null || itemData.getEnchantments().isEmpty()) {
            return "-";
        }

        return String.join("\n", itemData.getEnchantments());
    }

    public static List<String> formatItemList(List<LoggedItemData> items) {
        List<String> lines = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return lines;
        }

        for (LoggedItemData item : items) {
            lines.add("• " + formatSingleLine(item));
        }

        return lines;
    }

    private static void appendExtraInfo(StringBuilder line, LoggedItemData itemData) {
        List<String> extras = new ArrayList<>();

        if (itemData.isEnchanted() && !itemData.getEnchantments().isEmpty()) {
            extras.add("Enchants: " + String.join(", ", itemData.getEnchantments()));
        }

        if (itemData.hasSkullInfo()) {
            extras.add("Skull: " + itemData.getSkullInfo());
        }

        if (itemData.hasCustomModelData()) {
            extras.add("CMD: " + itemData.getCustomModelData());
        }

        if (!extras.isEmpty()) {
            line.append(" | ").append(String.join(" | ", extras));
        }
    }

    private static String resolveSkullInfo(ItemStack itemStack, ItemMeta meta) {
        if (itemStack == null || itemStack.getType() != Material.PLAYER_HEAD) {
            return "";
        }

        if (!(meta instanceof SkullMeta skullMeta)) {
            return "Custom Head";
        }

        try {
            if (skullMeta.getOwningPlayer() != null && skullMeta.getOwningPlayer().getName() != null) {
                return skullMeta.getOwningPlayer().getName();
            }
        } catch (Throwable ignored) {
        }

        try {
            if (skullMeta.getOwnerProfile() != null && skullMeta.getOwnerProfile().getName() != null) {
                return skullMeta.getOwnerProfile().getName();
            }
        } catch (Throwable ignored) {
        }

        try {
            if (skullMeta.hasOwner() && skullMeta.getOwner() != null && !skullMeta.getOwner().isBlank()) {
                return skullMeta.getOwner();
            }
        } catch (Throwable ignored) {
        }

        return "Custom Head";
    }

    private static Integer resolveCustomModelData(ItemMeta meta) {
        if (meta == null) {
            return null;
        }

        try {
            if (meta.hasCustomModelData()) {
                return meta.getCustomModelData();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}