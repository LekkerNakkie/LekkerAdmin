package me.lekkernakkie.lekkeradmin.model.application;

public enum ReviewDecision {
    APPROVE,
    DENY;

    public static ReviewDecision fromString(String input) {
        if (input == null || input.isBlank()) {
            return DENY;
        }

        try {
            return ReviewDecision.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DENY;
        }
    }
}