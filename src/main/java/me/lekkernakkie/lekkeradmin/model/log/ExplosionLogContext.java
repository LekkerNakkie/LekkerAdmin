package me.lekkernakkie.lekkeradmin.model.log;

public class ExplosionLogContext {

    private final String type;
    private final String triggeredBy;
    private final String worldName;
    private final String coordinates;
    private final String region;
    private final boolean cancelled;
    private final int destroyedBlockCount;
    private final String destroyedBlocksSummary;
    private final String containersSummary;
    private final int chainSize;
    private final String alertSummary;

    public ExplosionLogContext(
            String type,
            String triggeredBy,
            String worldName,
            String coordinates,
            String region,
            boolean cancelled,
            int destroyedBlockCount,
            String destroyedBlocksSummary,
            String containersSummary,
            int chainSize,
            String alertSummary
    ) {
        this.type = safe(type, "UNKNOWN");
        this.triggeredBy = safe(triggeredBy, "-");
        this.worldName = safe(worldName, "-");
        this.coordinates = safe(coordinates, "-");
        this.region = safe(region, "-");
        this.cancelled = cancelled;
        this.destroyedBlockCount = Math.max(0, destroyedBlockCount);
        this.destroyedBlocksSummary = safe(destroyedBlocksSummary, "-");
        this.containersSummary = containersSummary == null ? "" : containersSummary;
        this.chainSize = Math.max(1, chainSize);
        this.alertSummary = alertSummary == null ? "" : alertSummary;
    }

    public String getType() {
        return type;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public String getRegion() {
        return region;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public int getDestroyedBlockCount() {
        return destroyedBlockCount;
    }

    public String getDestroyedBlocksSummary() {
        return destroyedBlocksSummary;
    }

    public String getContainersSummary() {
        return containersSummary;
    }

    public int getChainSize() {
        return chainSize;
    }

    public String getAlertSummary() {
        return alertSummary;
    }

    public boolean hasContainers() {
        return containersSummary != null && !containersSummary.isBlank();
    }

    public boolean hasAlerts() {
        return alertSummary != null && !alertSummary.isBlank();
    }

    public String toPlainText() {
        StringBuilder builder = new StringBuilder();
        builder.append("[EXPLOSION] ")
                .append(type)
                .append(" | by=")
                .append(triggeredBy)
                .append(" | world=")
                .append(worldName)
                .append(" | coords=")
                .append(coordinates)
                .append(" | region=")
                .append(region)
                .append(" | cancelled=")
                .append(cancelled ? "Ja" : "Nee")
                .append(" | destroyed=")
                .append(destroyedBlockCount)
                .append(" | chain=")
                .append(chainSize);

        if (destroyedBlocksSummary != null && !destroyedBlocksSummary.isBlank()) {
            builder.append(" | blocks=").append(destroyedBlocksSummary);
        }

        if (containersSummary != null && !containersSummary.isBlank()) {
            builder.append(" | containers=").append(containersSummary);
        }

        if (alertSummary != null && !alertSummary.isBlank()) {
            builder.append(" | alert=").append(alertSummary);
        }

        return builder.toString();
    }

    private String safe(String input, String fallback) {
        return input == null || input.isBlank() ? fallback : input;
    }
}