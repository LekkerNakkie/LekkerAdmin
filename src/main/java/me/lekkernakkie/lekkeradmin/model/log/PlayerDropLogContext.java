package me.lekkernakkie.lekkeradmin.model.log;

public class PlayerDropLogContext {

    private final String playerName;
    private final String worldName;
    private final String coordinates;
    private final LoggedItemData itemData;
    private final String dropType;

    private String customSummary;
    private String destroyedItemsSummary;

    public PlayerDropLogContext(
            String playerName,
            String worldName,
            String coordinates,
            LoggedItemData itemData,
            String dropType
    ) {
        this.playerName = playerName == null ? "-" : playerName;
        this.worldName = worldName == null ? "-" : worldName;
        this.coordinates = coordinates == null ? "-" : coordinates;
        this.itemData = itemData;
        this.dropType = dropType == null ? "UNKNOWN" : dropType;
        this.customSummary = null;
        this.destroyedItemsSummary = null;
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

    public String getDropType() {
        return dropType;
    }

    public String getCustomSummary() {
        return customSummary;
    }

    public void setCustomSummary(String customSummary) {
        this.customSummary = customSummary;
    }

    public String getDestroyedItemsSummary() {
        return destroyedItemsSummary;
    }

    public void setDestroyedItemsSummary(String destroyedItemsSummary) {
        this.destroyedItemsSummary = destroyedItemsSummary;
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
        StringBuilder builder = new StringBuilder();
        builder.append("[DROP] ")
                .append(playerName)
                .append(" dropte ")
                .append(getItemSummary())
                .append(" @ ")
                .append(worldName)
                .append(" | ")
                .append(coordinates)
                .append(" | type=")
                .append(dropType);

        if (destroyedItemsSummary != null && !destroyedItemsSummary.isBlank()) {
            builder.append(" | destroyed=").append(destroyedItemsSummary);
        }

        return builder.toString();
    }
}