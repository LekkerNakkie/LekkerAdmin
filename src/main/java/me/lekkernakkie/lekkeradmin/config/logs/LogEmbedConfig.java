package me.lekkernakkie.lekkeradmin.config.logs;

public class LogEmbedConfig {

    private final String title;
    private final String description;
    private final String color;
    private final String footer;
    private final boolean useTimestamp;
    private final boolean showPlayerHeadThumbnail;
    private final boolean showEnchantments;
    private final boolean showLore;
    private final boolean showWorld;
    private final boolean showCoordinates;
    private final int maxFieldLength;
    private final LogFieldConfig fields;

    public LogEmbedConfig(
            String title,
            String description,
            String color,
            String footer,
            boolean useTimestamp,
            boolean showPlayerHeadThumbnail,
            boolean showEnchantments,
            boolean showLore,
            boolean showWorld,
            boolean showCoordinates,
            int maxFieldLength,
            LogFieldConfig fields
    ) {
        this.title = title == null ? "" : title;
        this.description = description == null ? "" : description;
        this.color = color == null || color.isBlank() ? "#5865F2" : color;
        this.footer = footer == null ? "" : footer;
        this.useTimestamp = useTimestamp;
        this.showPlayerHeadThumbnail = showPlayerHeadThumbnail;
        this.showEnchantments = showEnchantments;
        this.showLore = showLore;
        this.showWorld = showWorld;
        this.showCoordinates = showCoordinates;
        this.maxFieldLength = Math.max(32, maxFieldLength);
        this.fields = fields;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public String getFooter() {
        return footer;
    }

    public boolean isUseTimestamp() {
        return useTimestamp;
    }

    public boolean isShowPlayerHeadThumbnail() {
        return showPlayerHeadThumbnail;
    }

    public boolean isShowEnchantments() {
        return showEnchantments;
    }

    public boolean isShowLore() {
        return showLore;
    }

    public boolean isShowWorld() {
        return showWorld;
    }

    public boolean isShowCoordinates() {
        return showCoordinates;
    }

    public int getMaxFieldLength() {
        return maxFieldLength;
    }

    public LogFieldConfig getFields() {
        return fields;
    }
}