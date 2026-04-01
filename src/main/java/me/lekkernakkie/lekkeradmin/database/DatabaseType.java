package me.lekkernakkie.lekkeradmin.database;

public enum DatabaseType {
    SQLITE,
    MYSQL;

    public static DatabaseType fromString(String input) {
        if (input == null || input.isBlank()) {
            return SQLITE;
        }

        try {
            return DatabaseType.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SQLITE;
        }
    }
}