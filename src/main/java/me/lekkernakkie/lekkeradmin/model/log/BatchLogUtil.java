package me.lekkernakkie.lekkeradmin.util.log;

import me.lekkernakkie.lekkeradmin.model.log.ItemLogBatchEntry;
import me.lekkernakkie.lekkeradmin.model.log.LoggedItemData;
import me.lekkernakkie.lekkeradmin.model.log.MergedItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BatchLogUtil {

    private BatchLogUtil() {
    }

    public static List<MergedItem> merge(List<ItemLogBatchEntry> entries) {
        Map<String, MergedItem> map = new LinkedHashMap<>();

        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }

        for (ItemLogBatchEntry entry : entries) {
            if (entry == null || entry.getItemData() == null) {
                continue;
            }

            LoggedItemData item = entry.getItemData();
            String key = buildKey(item);

            map.computeIfAbsent(key, ignored -> new MergedItem(item)).add(entry);
        }

        return new ArrayList<>(map.values());
    }

    private static String buildKey(LoggedItemData item) {
        if (item == null) {
            return "null";
        }

        return item.getMaterial() + "|"
                + (item.getDisplayName() == null ? "" : item.getDisplayName()) + "|"
                + String.join(",", item.getEnchantments()) + "|"
                + (item.getSkullInfo() == null ? "" : item.getSkullInfo()) + "|"
                + (item.getCustomModelData() == null ? "" : item.getCustomModelData());
    }

    public static String buildSummary(List<MergedItem> merged, int maxShown) {
        List<String> lines = new ArrayList<>();
        if (merged == null || merged.isEmpty()) {
            return "-";
        }

        for (MergedItem item : merged) {
            if (item == null) {
                continue;
            }

            lines.add("• " + item.format());
        }

        return lines.isEmpty() ? "-" : String.join("\n", lines);
    }

    public static String buildDestroyedSummary(List<MergedItem> merged) {
        List<String> lines = new ArrayList<>();
        if (merged == null || merged.isEmpty()) {
            return "";
        }

        for (MergedItem item : merged) {
            if (item == null || item.getDestroyedAmount() <= 0) {
                continue;
            }

            lines.add("• " + item.formatDestroyed());
        }

        return lines.isEmpty() ? "" : String.join("\n", lines);
    }
}