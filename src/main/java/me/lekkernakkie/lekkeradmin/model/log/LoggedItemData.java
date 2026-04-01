package me.lekkernakkie.lekkeradmin.model.log;

import java.util.ArrayList;
import java.util.List;

public class LoggedItemData {

    private final String material;
    private final int amount;
    private final String displayName;
    private final List<String> enchantments;
    private final List<String> lore;
    private final boolean enchanted;
    private final String skullInfo;
    private final Integer customModelData;

    public LoggedItemData(
            String material,
            int amount,
            String displayName,
            List<String> enchantments,
            List<String> lore,
            boolean enchanted,
            String skullInfo,
            Integer customModelData
    ) {
        this.material = material == null ? "UNKNOWN" : material;
        this.amount = Math.max(0, amount);
        this.displayName = displayName == null ? "-" : displayName;
        this.enchantments = enchantments == null ? new ArrayList<>() : enchantments;
        this.lore = lore == null ? new ArrayList<>() : lore;
        this.enchanted = enchanted;
        this.skullInfo = skullInfo == null ? "" : skullInfo;
        this.customModelData = customModelData;
    }

    public String getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getEnchantments() {
        return enchantments;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean isEnchanted() {
        return enchanted;
    }

    public String getSkullInfo() {
        return skullInfo;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public boolean hasSkullInfo() {
        return skullInfo != null && !skullInfo.isBlank();
    }

    public boolean hasCustomModelData() {
        return customModelData != null;
    }
}