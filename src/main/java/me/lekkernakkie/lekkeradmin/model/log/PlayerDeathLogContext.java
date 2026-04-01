package me.lekkernakkie.lekkeradmin.model.log;

import java.util.ArrayList;
import java.util.List;

public class PlayerDeathLogContext {

    private final String playerName;
    private final String worldName;
    private final String coordinates;
    private final String cause;
    private final String killerName;
    private final String xpSummary;
    private final boolean keepInventory;
    private final String droppedItemsSummary;
    private final List<String> overflowDroppedItems;
    private final String destroyedItemsSummary;

    public PlayerDeathLogContext(
            String playerName,
            String worldName,
            String coordinates,
            String cause,
            String killerName,
            String xpSummary,
            boolean keepInventory,
            String droppedItemsSummary,
            List<String> overflowDroppedItems,
            String destroyedItemsSummary
    ) {
        this.playerName = playerName == null ? "-" : playerName;
        this.worldName = worldName == null ? "-" : worldName;
        this.coordinates = coordinates == null ? "-" : coordinates;
        this.cause = cause == null ? "Onbekend" : cause;
        this.killerName = killerName == null ? "-" : killerName;
        this.xpSummary = xpSummary == null ? "-" : xpSummary;
        this.keepInventory = keepInventory;
        this.droppedItemsSummary = droppedItemsSummary == null ? "-" : droppedItemsSummary;
        this.overflowDroppedItems = overflowDroppedItems == null ? new ArrayList<>() : overflowDroppedItems;
        this.destroyedItemsSummary = destroyedItemsSummary == null ? "" : destroyedItemsSummary;
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

    public String getCause() {
        return cause;
    }

    public String getKillerName() {
        return killerName;
    }

    public String getXpSummary() {
        return xpSummary;
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    public String getDroppedItemsSummary() {
        return droppedItemsSummary;
    }

    public List<String> getOverflowDroppedItems() {
        return overflowDroppedItems;
    }

    public String getDestroyedItemsSummary() {
        return destroyedItemsSummary;
    }

    public String toPlainText() {
        StringBuilder builder = new StringBuilder();
        builder.append("[DEATH] ")
                .append(playerName)
                .append(" stierf door ")
                .append(cause)
                .append(" @ ")
                .append(worldName)
                .append(" | ")
                .append(coordinates)
                .append(" | killer=")
                .append(killerName)
                .append(" | xp=")
                .append(xpSummary)
                .append(" | keepInventory=")
                .append(keepInventory ? "Ja" : "Nee")
                .append(" | drops=")
                .append(droppedItemsSummary);

        if (destroyedItemsSummary != null && !destroyedItemsSummary.isBlank()) {
            builder.append(" | destroyed=").append(destroyedItemsSummary);
        }

        return builder.toString();
    }
}