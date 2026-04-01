package me.lekkernakkie.lekkeradmin.discord.interaction.component;

public final class ComponentIdParser {

    private ComponentIdParser() {
    }

    public static ParsedComponentId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] split = raw.split(":", 3);

        String root = split.length > 0 ? split[0] : "";
        String action = split.length > 1 ? split[1] : "";
        String value = split.length > 2 ? split[2] : "";

        if (root.isBlank() || action.isBlank()) {
            return null;
        }

        return new ParsedComponentId(root, action, value);
    }

    public record ParsedComponentId(String root, String action, String value) {
    }
}