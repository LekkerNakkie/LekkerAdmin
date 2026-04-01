package me.lekkernakkie.lekkeradmin.model.log;

import java.util.ArrayList;
import java.util.List;

public class InvseeLogContext {

    private final String staffName;
    private final String targetName;
    private final boolean targetOnline;
    private final boolean enderChest;
    private final String openedAt;
    private final String closedAt;
    private final String duration;
    private final List<String> actions;

    public InvseeLogContext(
            String staffName,
            String targetName,
            boolean targetOnline,
            boolean enderChest,
            String openedAt,
            String closedAt,
            String duration,
            List<String> actions
    ) {
        this.staffName = staffName == null ? "-" : staffName;
        this.targetName = targetName == null ? "-" : targetName;
        this.targetOnline = targetOnline;
        this.enderChest = enderChest;
        this.openedAt = openedAt == null ? "-" : openedAt;
        this.closedAt = closedAt == null ? "-" : closedAt;
        this.duration = duration == null ? "-" : duration;
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }

    public String getStaffName() {
        return staffName;
    }

    public String getTargetName() {
        return targetName;
    }

    public boolean isTargetOnline() {
        return targetOnline;
    }

    public boolean isEnderChest() {
        return enderChest;
    }

    public String getOpenedAt() {
        return openedAt;
    }

    public String getClosedAt() {
        return closedAt;
    }

    public String getDuration() {
        return duration;
    }

    public List<String> getActions() {
        return actions;
    }

    public String getActionsSummary(int maxShown) {
        if (actions.isEmpty()) {
            return "Geen wijzigingen gemaakt.";
        }

        List<String> shown = new ArrayList<>();
        int limit = Math.max(1, maxShown);

        for (int i = 0; i < actions.size() && i < limit; i++) {
            shown.add("• " + actions.get(i));
        }

        if (actions.size() > limit) {
            shown.add("... en nog " + (actions.size() - limit) + " actie(s)");
        }

        return String.join("\n", shown);
    }

    public String getSessionTypeDisplay() {
        return enderChest ? "ENDERCHEST" : "INVSEE";
    }

    public String toPlainText() {
        return "[" + getSessionTypeDisplay() + "] staff=" + staffName
                + " | target=" + targetName
                + " | online=" + (targetOnline ? "Ja" : "Nee")
                + " | opened=" + openedAt
                + " | closed=" + closedAt
                + " | duration=" + duration
                + " | actions=" + getActionsSummary(20);
    }
}