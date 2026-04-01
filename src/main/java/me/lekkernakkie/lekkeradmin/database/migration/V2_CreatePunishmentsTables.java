package me.lekkernakkie.lekkeradmin.database.migration;

import me.lekkernakkie.lekkeradmin.database.DatabaseType;

import java.util.ArrayList;
import java.util.List;

public class V2_CreatePunishmentsTables {

    public String version() {
        return "V2";
    }

    public String description() {
        return "Create punishment tables and views";
    }

    public List<String> statements(DatabaseType databaseType) {
        return databaseType == DatabaseType.MYSQL ? mysqlStatements() : sqliteStatements();
    }

    private List<String> sqliteStatements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS punishments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                    discord_name TEXT,
                    discord_id TEXT,
                    minecraft_name TEXT NOT NULL,
                    minecraft_uuid TEXT,

                    punishment_type TEXT NOT NULL,
                    reason TEXT NOT NULL,

                    issued_by_name TEXT,
                    issued_by_uuid TEXT,
                    issued_by_discord_name TEXT,
                    issued_by_discord_id TEXT,
                    issued_source TEXT NOT NULL,

                    duration_ms INTEGER,
                    issued_at INTEGER NOT NULL,
                    expires_at INTEGER,
                    status TEXT NOT NULL,

                    removed_at INTEGER,
                    removed_by_name TEXT,
                    removed_by_uuid TEXT,
                    removed_by_discord_name TEXT,
                    removed_by_discord_id TEXT,
                    remove_reason TEXT,

                    server_name TEXT,

                    notify_on_join INTEGER NOT NULL DEFAULT 0,
                    notification_delivered INTEGER NOT NULL DEFAULT 0
                )
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_punishments_minecraft_name
                ON punishments(minecraft_name)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_punishments_minecraft_uuid
                ON punishments(minecraft_uuid)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_punishments_discord_id
                ON punishments(discord_id)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_punishments_type_status
                ON punishments(punishment_type, status)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_punishments_expires_at
                ON punishments(expires_at)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_punishments_notify_on_join
                ON punishments(minecraft_uuid, notify_on_join, notification_delivered)
                """);

        statements.add("""
                CREATE VIEW IF NOT EXISTS active_punishments AS
                SELECT *
                FROM punishments
                WHERE status = 'ACTIVE'
                """);

        statements.add("""
                CREATE VIEW IF NOT EXISTS expired_punishments AS
                SELECT *
                FROM punishments
                WHERE status = 'EXPIRED'
                """);

        return statements;
    }

    private List<String> mysqlStatements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS punishments (
                    id BIGINT NOT NULL AUTO_INCREMENT,

                    discord_name VARCHAR(100) NULL,
                    discord_id VARCHAR(64) NULL,
                    minecraft_name VARCHAR(16) NOT NULL,
                    minecraft_uuid VARCHAR(36) NULL,

                    punishment_type VARCHAR(20) NOT NULL,
                    reason TEXT NOT NULL,

                    issued_by_name VARCHAR(100) NULL,
                    issued_by_uuid VARCHAR(36) NULL,
                    issued_by_discord_name VARCHAR(100) NULL,
                    issued_by_discord_id VARCHAR(64) NULL,
                    issued_source VARCHAR(20) NOT NULL,

                    duration_ms BIGINT NULL,
                    issued_at BIGINT NOT NULL,
                    expires_at BIGINT NULL,
                    status VARCHAR(20) NOT NULL,

                    removed_at BIGINT NULL,
                    removed_by_name VARCHAR(100) NULL,
                    removed_by_uuid VARCHAR(36) NULL,
                    removed_by_discord_name VARCHAR(100) NULL,
                    removed_by_discord_id VARCHAR(64) NULL,
                    remove_reason TEXT NULL,

                    server_name VARCHAR(100) NULL,

                    notify_on_join TINYINT(1) NOT NULL DEFAULT 0,
                    notification_delivered TINYINT(1) NOT NULL DEFAULT 0,

                    PRIMARY KEY (id),
                    INDEX idx_punishments_minecraft_name (minecraft_name),
                    INDEX idx_punishments_minecraft_uuid (minecraft_uuid),
                    INDEX idx_punishments_discord_id (discord_id),
                    INDEX idx_punishments_type_status (punishment_type, status),
                    INDEX idx_punishments_expires_at (expires_at),
                    INDEX idx_punishments_notify_on_join (minecraft_uuid, notify_on_join, notification_delivered)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        statements.add("""
                CREATE OR REPLACE VIEW active_punishments AS
                SELECT *
                FROM punishments
                WHERE status = 'ACTIVE'
                """);

        statements.add("""
                CREATE OR REPLACE VIEW expired_punishments AS
                SELECT *
                FROM punishments
                WHERE status = 'EXPIRED'
                """);

        return statements;
    }
}