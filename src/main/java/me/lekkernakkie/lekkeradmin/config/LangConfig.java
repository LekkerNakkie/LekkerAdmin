package me.lekkernakkie.lekkeradmin.config;

import me.lekkernakkie.lekkeradmin.util.StringUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LangConfig {

    private final FileConfiguration config;

    public LangConfig(FileConfiguration config) {
        this.config = config;
    }

    public String getRaw(String path, String fallback) {
        return config.getString(path, fallback);
    }

    public String get(String path, String fallback) {
        return StringUtil.colorize(getRaw(path, fallback));
    }

    public List<String> getList(String path, List<String> fallback) {
        List<String> list = config.getStringList(path);
        if (list == null || list.isEmpty()) {
            if (fallback == null) {
                return Collections.emptyList();
            }

            List<String> coloredFallback = new ArrayList<>(fallback.size());
            for (String line : fallback) {
                coloredFallback.add(StringUtil.colorize(line));
            }
            return coloredFallback;
        }

        List<String> colored = new ArrayList<>(list.size());
        for (String line : list) {
            colored.add(StringUtil.colorize(line));
        }
        return colored;
    }

    public String getPrefix() {
        return get("general.prefix", "&7[&9LekkerAdmin&7] ");
    }

    public String message(String path, String fallback) {
        return withPrefix(get(path, fallback));
    }

    public List<String> messageList(String path, List<String> fallback) {
        List<String> lines = getList(path, fallback);
        if (lines.isEmpty()) {
            return lines;
        }

        List<String> result = new ArrayList<>(lines.size());
        String prefix = getPrefix();

        for (int i = 0; i < lines.size(); i++) {
            if (i == 0) {
                result.add(prefix + lines.get(i));
            } else {
                result.add(lines.get(i));
            }
        }

        return result;
    }

    public String withPrefix(String message) {
        return getPrefix() + message;
    }

    public String format(String path, String fallback, Map<String, String> placeholders) {
        return applyPlaceholders(get(path, fallback), placeholders);
    }

    public String formatMessage(String path, String fallback, Map<String, String> placeholders) {
        return withPrefix(format(path, fallback, placeholders));
    }

    public List<String> formatMessageList(String path, List<String> fallback, Map<String, String> placeholders) {
        List<String> lines = getList(path, fallback);
        List<String> result = new ArrayList<>(lines.size());

        for (int i = 0; i < lines.size(); i++) {
            String line = applyPlaceholders(lines.get(i), placeholders);
            if (i == 0) {
                result.add(getPrefix() + line);
            } else {
                result.add(line);
            }
        }

        return result;
    }

    private String applyPlaceholders(String input, Map<String, String> placeholders) {
        String output = input == null ? "" : input;
        if (placeholders == null || placeholders.isEmpty()) {
            return output;
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            output = output.replace("{" + key + "}", value);
        }

        return output;
    }
}