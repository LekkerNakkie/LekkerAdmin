package me.lekkernakkie.lekkeradmin.config;

import me.lekkernakkie.lekkeradmin.util.StringUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    public String getPrefix() {
        return getRaw("general.prefix", "&7[&9LekkerAdmin&7] ");
    }

    public String getSectionPrefix(String section) {
        return getRaw(section + ".prefix", "");
    }

    public String withPrefix(String message) {
        if (message == null) {
            return StringUtil.colorize(getPrefix());
        }
        return StringUtil.colorize(getPrefix() + message);
    }

    public String get(String path, String fallback) {
        return StringUtil.colorize(applyPlaceholders(getRaw(path, fallback), basePlaceholders()));
    }

    public List<String> getList(String path, List<String> fallback) {
        List<String> list = config.getStringList(path);
        List<String> source = (list == null || list.isEmpty()) ? fallback : list;

        if (source == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>(source.size());
        Map<String, String> placeholders = basePlaceholders();

        for (String line : source) {
            result.add(StringUtil.colorize(applyPlaceholders(line, placeholders)));
        }

        return result;
    }

    public String message(String path, String fallback) {
        return format(path, fallback, null);
    }

    public List<String> messageList(String path, List<String> fallback) {
        return formatMessageList(path, fallback, null);
    }

    public String format(String path, String fallback, Map<String, String> placeholders) {
        Map<String, String> merged = merge(placeholders);
        return StringUtil.colorize(applyPlaceholders(getRaw(path, fallback), merged));
    }

    public String formatMessage(String path, String fallback, Map<String, String> placeholders) {
        return format(path, fallback, placeholders);
    }

    public List<String> formatMessageList(String path, List<String> fallback, Map<String, String> placeholders) {
        List<String> list = config.getStringList(path);
        List<String> source = (list == null || list.isEmpty()) ? fallback : list;

        if (source == null) {
            return Collections.emptyList();
        }

        Map<String, String> merged = merge(placeholders);
        List<String> result = new ArrayList<>(source.size());

        for (String line : source) {
            result.add(StringUtil.colorize(applyPlaceholders(line, merged)));
        }

        return result;
    }

    private Map<String, String> basePlaceholders() {
        return merge(null);
    }

    private Map<String, String> merge(Map<String, String> placeholders) {
        Map<String, String> map = new HashMap<>();

        if (placeholders != null) {
            map.putAll(placeholders);
        }

        map.putIfAbsent("prefix", StringUtil.colorize(getPrefix()));
        map.putIfAbsent("general-prefix", StringUtil.colorize(getPrefix()));
        map.putIfAbsent("restart-prefix", StringUtil.colorize(getSectionPrefix("restart")));
        map.putIfAbsent("punishments-prefix", StringUtil.colorize(getSectionPrefix("punishments")));
        map.putIfAbsent("vanish-prefix", StringUtil.colorize(getSectionPrefix("vanish")));
        map.putIfAbsent("whois-prefix", StringUtil.colorize(getSectionPrefix("whois")));
        map.putIfAbsent("maintenance-prefix", StringUtil.colorize(getSectionPrefix("maintenance")));

        return map;
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