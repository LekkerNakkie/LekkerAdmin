package me.lekkernakkie.lekkeradmin.config;

import me.lekkernakkie.lekkeradmin.util.StringUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

    public String getSectionPrefix(String section) {
        return get(section + ".prefix", "");
    }

    public String message(String path, String fallback) {
        return get(path, fallback);
    }

    public List<String> messageList(String path, List<String> fallback) {
        return getList(path, fallback);
    }

    public String format(String path, String fallback, Map<String, String> placeholders) {
        return StringUtil.colorize(applyPlaceholders(getRaw(path, fallback), placeholders));
    }

    public String formatMessage(String path, String fallback, Map<String, String> placeholders) {
        return format(path, fallback, placeholders);
    }

    public List<String> formatMessageList(String path, List<String> fallback, Map<String, String> placeholders) {
        List<String> lines = config.getStringList(path);
        List<String> source = (lines == null || lines.isEmpty()) ? fallback : lines;

        if (source == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>(source.size());
        for (String line : source) {
            result.add(StringUtil.colorize(applyPlaceholders(line, placeholders)));
        }
        return result;
    }

    public Map<String, String> withPrefixes(Map<String, String> placeholders) {
        Map<String, String> result = new HashMap<>();
        if (placeholders != null) {
            result.putAll(placeholders);
        }

        result.putIfAbsent("prefix", getPrefix());
        result.putIfAbsent("general-prefix", getPrefix());
        result.putIfAbsent("restart-prefix", getSectionPrefix("restart"));
        result.putIfAbsent("punishments-prefix", getSectionPrefix("punishments"));
        result.putIfAbsent("vanish-prefix", getSectionPrefix("vanish"));
        result.putIfAbsent("whois-prefix", getSectionPrefix("whois"));
        result.putIfAbsent("maintenance-prefix", getSectionPrefix("maintenance"));

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