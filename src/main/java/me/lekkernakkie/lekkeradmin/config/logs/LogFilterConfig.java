package me.lekkernakkie.lekkeradmin.config.logs;

import java.util.ArrayList;
import java.util.List;

public class LogFilterConfig {

    private final List<String> ignoredWorlds;
    private final List<String> ignoredMaterials;
    private final boolean onlyLogIfEnchanted;
    private final int minAmount;
    private final boolean splitLargeItemLists;
    private final int maxItemsShown;
    private final boolean deduplicateDeathDrops;

    public LogFilterConfig(
            List<String> ignoredWorlds,
            List<String> ignoredMaterials,
            boolean onlyLogIfEnchanted,
            int minAmount,
            boolean splitLargeItemLists,
            int maxItemsShown,
            boolean deduplicateDeathDrops
    ) {
        this.ignoredWorlds = ignoredWorlds == null ? new ArrayList<>() : ignoredWorlds;
        this.ignoredMaterials = ignoredMaterials == null ? new ArrayList<>() : ignoredMaterials;
        this.onlyLogIfEnchanted = onlyLogIfEnchanted;
        this.minAmount = Math.max(1, minAmount);
        this.splitLargeItemLists = splitLargeItemLists;
        this.maxItemsShown = Math.max(1, maxItemsShown);
        this.deduplicateDeathDrops = deduplicateDeathDrops;
    }

    public List<String> getIgnoredWorlds() {
        return ignoredWorlds;
    }

    public List<String> getIgnoredMaterials() {
        return ignoredMaterials;
    }

    public boolean isOnlyLogIfEnchanted() {
        return onlyLogIfEnchanted;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public boolean isSplitLargeItemLists() {
        return splitLargeItemLists;
    }

    public int getMaxItemsShown() {
        return maxItemsShown;
    }

    public boolean isDeduplicateDeathDrops() {
        return deduplicateDeathDrops;
    }
}