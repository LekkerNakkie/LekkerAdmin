package me.lekkernakkie.lekkeradmin.config.logs;

public class LogAggregationConfig {

    private final boolean enabled;
    private final int flushAfterTicks;
    private final boolean trackDestruction;
    private final boolean trackPickups;
    private final boolean includeDestroyedItems;

    public LogAggregationConfig(
            boolean enabled,
            int flushAfterTicks,
            boolean trackDestruction,
            boolean trackPickups,
            boolean includeDestroyedItems
    ) {
        this.enabled = enabled;
        this.flushAfterTicks = Math.max(1, flushAfterTicks);
        this.trackDestruction = trackDestruction;
        this.trackPickups = trackPickups;
        this.includeDestroyedItems = includeDestroyedItems;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getFlushAfterTicks() {
        return flushAfterTicks;
    }

    public boolean isTrackDestruction() {
        return trackDestruction;
    }

    public boolean isTrackPickups() {
        return trackPickups;
    }

    public boolean isIncludeDestroyedItems() {
        return includeDestroyedItems;
    }
}