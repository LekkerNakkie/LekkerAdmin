package me.lekkernakkie.lekkeradmin.util.log;

import me.lekkernakkie.lekkeradmin.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public final class LogFormatUtil {

    private LogFormatUtil() {
    }

    public static String safe(String input) {
        return StringUtil.safe(input);
    }

    public static String limit(String input, int maxLength) {
        return StringUtil.limit(StringUtil.safe(input), maxLength);
    }

    public static String yesNo(boolean value) {
        return value ? "Ja" : "Nee";
    }

    public static String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "-";
        }

        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(line);
        }

        return builder.isEmpty() ? "-" : builder.toString();
    }

    public static List<String> splitOverflow(List<String> lines, int maxShown) {
        List<String> overflow = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            return overflow;
        }

        if (maxShown < 1) {
            overflow.addAll(lines);
            return overflow;
        }

        for (int i = maxShown; i < lines.size(); i++) {
            overflow.add(lines.get(i));
        }

        return overflow;
    }

    public static List<String> takeFirst(List<String> lines, int maxShown) {
        List<String> result = new ArrayList<>();
        if (lines == null || lines.isEmpty() || maxShown <= 0) {
            return result;
        }

        for (int i = 0; i < lines.size() && i < maxShown; i++) {
            result.add(lines.get(i));
        }

        return result;
    }
}