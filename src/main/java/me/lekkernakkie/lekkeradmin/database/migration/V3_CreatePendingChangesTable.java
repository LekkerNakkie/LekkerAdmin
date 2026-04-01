package me.lekkernakkie.lekkeradmin.database.migration;

import me.lekkernakkie.lekkeradmin.database.DatabaseType;

import java.util.ArrayList;
import java.util.List;

public class V3_CreatePendingChangesTable {

    public String version() {
        return "V3";
    }

    public String description() {
        return "Create pending_changes table for offline invsee and enderchest persistence";
    }

    public List<String> statements(DatabaseType databaseType) {
        return databaseType == DatabaseType.MYSQL ? mysqlStatements() : sqliteStatements();
    }

    private List<String> sqliteStatements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS pending_changes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                    minecraft_name TEXT NOT NULL,
                    minecraft_uuid TEXT NOT NULL UNIQUE,

                    discord_name TEXT,
                    discord_id TEXT,

                    inventory_data LONGTEXT NOT NULL,
                    enderchest_data LONGTEXT NOT NULL,

                    last_online TEXT,
                    last_edited TEXT,
                    last_applied TEXT,
                    edited_by TEXT,

                    has_pending_changes INTEGER NOT NULL DEFAULT 0
                )
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_pending_changes_minecraft_name
                ON pending_changes(minecraft_name)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_pending_changes_discord_id
                ON pending_changes(discord_id)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_pending_changes_has_pending_changes
                ON pending_changes(has_pending_changes)
                """);

        return statements;
    }

    private List<String> mysqlStatements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS pending_changes (
                    id BIGINT NOT NULL AUTO_INCREMENT,

                    minecraft_name VARCHAR(16) NOT NULL,
                    minecraft_uuid VARCHAR(36) NOT NULL,

                    discord_name VARCHAR(100) NULL,
                    discord_id VARCHAR(64) NULL,

                    inventory_data LONGTEXT NOT NULL,
                    enderchest_data LONGTEXT NOT NULL,

                    last_online DATETIME(3) NULL,
                    last_edited DATETIME(3) NULL,
                    last_applied DATETIME(3) NULL,
                    edited_by VARCHAR(100) NULL,

                    has_pending_changes TINYINT(1) NOT NULL DEFAULT 0,

                    PRIMARY KEY (id),
                    UNIQUE KEY uk_pending_changes_minecraft_uuid (minecraft_uuid),
                    INDEX idx_pending_changes_minecraft_name (minecraft_name),
                    INDEX idx_pending_changes_discord_id (discord_id),
                    INDEX idx_pending_changes_has_pending_changes (has_pending_changes)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        return statements;
    }
}