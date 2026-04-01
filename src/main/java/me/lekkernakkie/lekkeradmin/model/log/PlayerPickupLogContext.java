package me.lekkernakkie.lekkeradmin.model.log;

public class PlayerPickupLogContext {

    private final String playerName;
    private final String worldName;
    private final String coordinates;
    private final LoggedItemData itemData;
    private final String pickupType;

    private String customSummary;

    public PlayerPickupLogContext(
            String playerName,
            String worldName,
            String coordinates,
            LoggedItemData itemData,
            String pickupType
    ) {
        this.playerName = playerName == null ? "-" : playerName;
        this.worldName = worldName == null ? "-" : worldName;
        this.coordinates = coordinates == null ? "-" : coordinates;
        this.itemData = itemData;
        this.pickupType = pickupType == null ? "GROUND_PICKUP" : pickupType;
        this.customSummary = null;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public LoggedItemData getItemData() {
        return itemData;
    }

    public String getPickupType() {
        return pickupType;
    }

    public String getCustomSummary() {
        return customSummary;
    }

    public void setCustomSummary(String customSummary) {
        this.customSummary = customSummary;
    }

    public int getAmount() {
        return itemData == null ? 0 : itemData.getAmount();
    }

    public String getItemSummary() {
        if (customSummary != null && !customSummary.isBlank()) {
            return customSummary;
        }

        if (itemData == null) {
            return "-";
        }

        if (itemData.getDisplayName() != null
                && !itemData.getDisplayName().isBlank()
                && !itemData.getDisplayName().equalsIgnoreCase(itemData.getMaterial())) {
            return itemData.getDisplayName() + " (" + itemData.getMaterial() + ") x" + itemData.getAmount();
        }

        return itemData.getMaterial() + " x" + itemData.getAmount();
    }

    public boolean hasEnchantments() {
        return itemData != null && itemData.getEnchantments() != null && !itemData.getEnchantments().isEmpty();
    }

    public String getEnchantmentsSummary() {
        if (!hasEnchantments()) {
            return "-";
        }

        return String.join("\n", itemData.getEnchantments());
    }

    public String toPlainText() {
        return "[PICKUP] "
                + playerName
                + " pakte "
                + getItemSummary()
                + " op @ "
                + worldName
                + " | "
                + coordinates
                + " | type="
                + pickupType;
    }
}