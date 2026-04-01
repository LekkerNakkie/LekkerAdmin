package me.lekkernakkie.lekkeradmin.model.log;

import java.util.ArrayList;
import java.util.List;

public class MergedItem {

    private final LoggedItemData base;
    private int totalAmount;
    private int destroyedAmount;
    private int pickedUpAmount;

    public MergedItem(LoggedItemData base) {
        this.base = base;
        this.totalAmount = 0;
        this.destroyedAmount = 0;
        this.pickedUpAmount = 0;
    }

    public void add(ItemLogBatchEntry entry) {
        if (entry == null || entry.getItemData() == null) {
            return;
        }

        int amount = entry.getItemData().getAmount();
        totalAmount += amount;

        if (entry.isDestroyed()) {
            destroyedAmount += amount;
        }

        if (entry.isPickedUp()) {
            pickedUpAmount += amount;
        }
    }

    public LoggedItemData getBase() {
        return base;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public int getDestroyedAmount() {
        return destroyedAmount;
    }

    public int getPickedUpAmount() {
        return pickedUpAmount;
    }

    public String format() {
        String name = base.getDisplayName() != null && !base.getDisplayName().isBlank()
                ? base.getDisplayName()
                : base.getMaterial();

        StringBuilder line = new StringBuilder();
        line.append(name).append(" x").append(totalAmount);

        List<String> extras = new ArrayList<>();

        if (!base.getDisplayName().equalsIgnoreCase(base.getMaterial())) {
            extras.add("Material: " + base.getMaterial());
        }

        if (!base.getEnchantments().isEmpty()) {
            extras.add("Enchants: " + String.join(", ", base.getEnchantments()));
        }

        if (base.hasSkullInfo()) {
            extras.add("Skull: " + base.getSkullInfo());
        }

        if (base.hasCustomModelData()) {
            extras.add("CMD: " + base.getCustomModelData());
        }

        if (!extras.isEmpty()) {
            line.append(" | ").append(String.join(" | ", extras));
        }

        return line.toString();
    }

    public String formatDestroyed() {
        String name = base.getDisplayName() != null && !base.getDisplayName().isBlank()
                ? base.getDisplayName()
                : base.getMaterial();

        StringBuilder line = new StringBuilder();
        line.append(name).append(" x").append(destroyedAmount);

        List<String> extras = new ArrayList<>();

        if (!base.getDisplayName().equalsIgnoreCase(base.getMaterial())) {
            extras.add("Material: " + base.getMaterial());
        }

        if (!base.getEnchantments().isEmpty()) {
            extras.add("Enchants: " + String.join(", ", base.getEnchantments()));
        }

        if (base.hasSkullInfo()) {
            extras.add("Skull: " + base.getSkullInfo());
        }

        if (base.hasCustomModelData()) {
            extras.add("CMD: " + base.getCustomModelData());
        }

        if (!extras.isEmpty()) {
            line.append(" | ").append(String.join(" | ", extras));
        }

        return line.toString();
    }
}