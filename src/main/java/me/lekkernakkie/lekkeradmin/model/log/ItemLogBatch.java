package me.lekkernakkie.lekkeradmin.model.log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemLogBatch {

    private final UUID batchId;
    private final UUID playerUuid;
    private final String playerName;
    private final String worldName;
    private final String coordinates;
    private final ItemLogBatchType type;
    private final long createdAt;

    private final List<ItemLogBatchEntry> entries;

    private String deathCause;
    private String killerName;
    private String xpSummary;
    private boolean keepInventory;

    private int scheduledTaskId = -1;

    public ItemLogBatch(
            UUID batchId,
            UUID playerUuid,
            String playerName,
            String worldName,
            String coordinates,
            ItemLogBatchType type,
            long createdAt
    ) {
        this.batchId = batchId;
        this.playerUuid = playerUuid;
        this.playerName = playerName == null ? "-" : playerName;
        this.worldName = worldName == null ? "-" : worldName;
        this.coordinates = coordinates == null ? "-" : coordinates;
        this.type = type;
        this.createdAt = createdAt;
        this.entries = new ArrayList<>();
        this.deathCause = "-";
        this.killerName = "-";
        this.xpSummary = "-";
        this.keepInventory = false;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
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

    public ItemLogBatchType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public List<ItemLogBatchEntry> getEntries() {
        return entries;
    }

    public void addEntry(ItemLogBatchEntry entry) {
        if (entry != null) {
            this.entries.add(entry);
        }
    }

    public String getDeathCause() {
        return deathCause;
    }

    public void setDeathCause(String deathCause) {
        this.deathCause = deathCause == null ? "-" : deathCause;
    }

    public String getKillerName() {
        return killerName;
    }

    public void setKillerName(String killerName) {
        this.killerName = killerName == null ? "-" : killerName;
    }

    public String getXpSummary() {
        return xpSummary;
    }

    public void setXpSummary(String xpSummary) {
        this.xpSummary = xpSummary == null ? "-" : xpSummary;
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    public void setKeepInventory(boolean keepInventory) {
        this.keepInventory = keepInventory;
    }

    public int getScheduledTaskId() {
        return scheduledTaskId;
    }

    public void setScheduledTaskId(int scheduledTaskId) {
        this.scheduledTaskId = scheduledTaskId;
    }
}