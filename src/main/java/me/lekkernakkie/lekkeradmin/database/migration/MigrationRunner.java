package me.lekkernakkie.lekkeradmin.database.migration;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MigrationRunner {

    private final LekkerAdmin plugin;
    private final DatabaseManager databaseManager;
    private final DCBotConfig config;

    public MigrationRunner(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public MigrationRunner(LekkerAdmin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public void runMigrations() {
        ensureMigrationsTable();

        List<Migration> migrations = List.of(
                new Migration("V1", "Single whitelist_entries schema", getV1Statements()),
                new Migration("V2", "Create punishments table and indexes", getV2Statements()),
                new Migration("V3", "Create pending_changes table", getV3Statements())
        );

        for (Migration migration : migrations) {
            if (hasMigration(migration.version())) {
                continue;
            }
            runMigration(migration);
        }

        ensureWhitelistEntriesColumns();
        ensureWhitelistEntriesNonUniqueConstraints();
        ensurePunishmentsColumns();
        ensurePunishmentsIndexes();
        ensurePendingChangesColumns();
        ensurePendingChangesIndexes();
    }

    private void ensureMigrationsTable() {
        String databaseType = normalizeDatabaseType();

        String sql;
        if (databaseType.equals("MYSQL")) {
            sql = """
                    CREATE TABLE IF NOT EXISTS lekkeradmin_migrations (
                        version VARCHAR(32) NOT NULL PRIMARY KEY,
                        description VARCHAR(255) NOT NULL,
                        applied_at BIGINT NOT NULL
                    )
                    """;
        } else {
            sql = """
                    CREATE TABLE IF NOT EXISTS lekkeradmin_migrations (
                        version TEXT NOT NULL PRIMARY KEY,
                        description TEXT NOT NULL,
                        applied_at INTEGER NOT NULL
                    )
                    """;
        }

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create migrations table", ex);
        }
    }

    private boolean hasMigration(String version) {
        String sql = "SELECT version FROM lekkeradmin_migrations WHERE version = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, version);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to check migration: " + version, ex);
        }
    }

    private void runMigration(Migration migration) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                for (String sql : migration.statements()) {
                    statement.executeUpdate(sql);
                }

                insertMigrationRow(connection, migration);

                connection.commit();
                plugin.debug("Applied database migration: " + migration.version());

            } catch (Exception ex) {
                connection.rollback();
                throw new RuntimeException("Failed to run migration: " + migration.version(), ex);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to run migration: " + migration.version(), ex);
        }
    }

    private void insertMigrationRow(Connection connection, Migration migration) throws Exception {
        boolean hasDescription = migrationsTableHasDescriptionColumn(connection);
        String appliedAtType = getAppliedAtColumnType(connection);

        String sql = hasDescription
                ? "INSERT INTO lekkeradmin_migrations (version, description, applied_at) VALUES (?, ?, ?)"
                : "INSERT INTO lekkeradmin_migrations (version, applied_at) VALUES (?, ?)";

        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            insert.setString(1, migration.version());

            int appliedAtIndex;
            if (hasDescription) {
                insert.setString(2, migration.description());
                appliedAtIndex = 3;
            } else {
                appliedAtIndex = 2;
            }

            if (isDateTimeType(appliedAtType)) {
                insert.setTimestamp(appliedAtIndex, new Timestamp(System.currentTimeMillis()));
            } else {
                insert.setLong(appliedAtIndex, System.currentTimeMillis());
            }

            insert.executeUpdate();
        }
    }

    private boolean migrationsTableHasDescriptionColumn(Connection connection) {
        String databaseType = normalizeDatabaseType();

        String sql;
        if (databaseType.equals("MYSQL")) {
            sql = """
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'lekkeradmin_migrations'
                      AND COLUMN_NAME = 'description'
                    """;
        } else {
            sql = "PRAGMA table_info(lekkeradmin_migrations)";
        }

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (databaseType.equals("MYSQL")) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }

            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("description".equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
            return false;

        } catch (Exception ex) {
            throw new RuntimeException("Failed to inspect migrations table schema", ex);
        }
    }

    private String getAppliedAtColumnType(Connection connection) {
        String databaseType = normalizeDatabaseType();

        String sql;
        if (databaseType.equals("MYSQL")) {
            sql = """
                    SELECT DATA_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'lekkeradmin_migrations'
                      AND COLUMN_NAME = 'applied_at'
                    """;
        } else {
            sql = "PRAGMA table_info(lekkeradmin_migrations)";
        }

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (databaseType.equals("MYSQL")) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return "bigint";
            }

            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("applied_at".equalsIgnoreCase(columnName)) {
                    return rs.getString("type");
                }
            }
            return "integer";

        } catch (Exception ex) {
            throw new RuntimeException("Failed to inspect applied_at column type", ex);
        }
    }

    private boolean isDateTimeType(String type) {
        if (type == null) {
            return false;
        }

        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("datetime")
                || normalized.contains("timestamp")
                || normalized.contains("date");
    }

    private List<String> getV1Statements() {
        return normalizeDatabaseType().equals("MYSQL") ? getMySqlV1Statements() : getSqliteV1Statements();
    }

    private List<String> getV2Statements() {
        return normalizeDatabaseType().equals("MYSQL") ? getMySqlV2Statements() : getSqliteV2Statements();
    }

    private List<String> getV3Statements() {
        return normalizeDatabaseType().equals("MYSQL") ? getMySqlV3Statements() : getSqliteV3Statements();
    }

    private List<String> getSqliteV1Statements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS whitelist_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    application_id TEXT NOT NULL UNIQUE,
                    discord_user_id TEXT NOT NULL UNIQUE,
                    discord_tag TEXT NOT NULL,
                    minecraft_name TEXT NOT NULL UNIQUE,
                    minecraft_uuid TEXT,
                    status TEXT NOT NULL,
                    review_reason TEXT,
                    reviewed_by_discord_id TEXT,
                    reviewed_by_discord_name TEXT,
                    submitted_at INTEGER NOT NULL,
                    reviewed_at INTEGER,
                    linked_at INTEGER,
                    finalized_at INTEGER,
                    name_retry_count INTEGER NOT NULL DEFAULT 0,
                    form_answers_json TEXT NOT NULL
                )
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_whitelist_entries_status
                ON whitelist_entries(status)
                """);

        statements.add("""
                CREATE TABLE IF NOT EXISTS audit_log_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action_type TEXT NOT NULL,
                    actor_id TEXT,
                    actor_name TEXT,
                    target_id TEXT,
                    target_name TEXT,
                    application_id TEXT,
                    details TEXT,
                    created_at INTEGER NOT NULL
                )
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_audit_application_id
                ON audit_log_entries(application_id)
                """);

        return statements;
    }

    private List<String> getMySqlV1Statements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS whitelist_entries (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    application_id VARCHAR(64) NOT NULL,
                    discord_user_id VARCHAR(64) NOT NULL,
                    discord_tag VARCHAR(100) NOT NULL,
                    minecraft_name VARCHAR(16) NOT NULL,
                    minecraft_uuid VARCHAR(36) NULL,
                    status VARCHAR(50) NOT NULL,
                    review_reason TEXT NULL,
                    reviewed_by_discord_id VARCHAR(64) NULL,
                    reviewed_by_discord_name VARCHAR(100) NULL,
                    submitted_at BIGINT NOT NULL,
                    reviewed_at BIGINT NULL,
                    linked_at BIGINT NULL,
                    finalized_at BIGINT NULL,
                    name_retry_count INT NOT NULL DEFAULT 0,
                    form_answers_json LONGTEXT NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_whitelist_entries_application_id (application_id),
                    UNIQUE KEY uk_whitelist_entries_discord_user_id (discord_user_id),
                    UNIQUE KEY uk_whitelist_entries_minecraft_name (minecraft_name),
                    INDEX idx_whitelist_entries_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        statements.add("""
                CREATE TABLE IF NOT EXISTS audit_log_entries (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    action_type VARCHAR(100) NOT NULL,
                    actor_id VARCHAR(64) NULL,
                    actor_name VARCHAR(100) NULL,
                    target_id VARCHAR(64) NULL,
                    target_name VARCHAR(100) NULL,
                    application_id VARCHAR(64) NULL,
                    details TEXT NULL,
                    created_at BIGINT NOT NULL,
                    PRIMARY KEY (id),
                    INDEX idx_audit_application_id (application_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        return statements;
    }

    private List<String> getSqliteV2Statements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS punishments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    discord_name TEXT,
                    discord_id TEXT,
                    minecraft_name TEXT NOT NULL,
                    minecraft_uuid TEXT,
                    punishment_type TEXT NOT NULL,
                    reason TEXT,
                    issued_by_name TEXT,
                    issued_by_uuid TEXT,
                    issued_by_discord_name TEXT,
                    issued_by_discord_id TEXT,
                    issued_source TEXT,
                    duration_ms INTEGER,
                    issued_at TEXT NOT NULL,
                    expires_at TEXT,
                    status TEXT NOT NULL,
                    removed_at TEXT,
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
                CREATE INDEX IF NOT EXISTS idx_punishments_minecraft_uuid
                ON punishments(minecraft_uuid)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_punishments_status
                ON punishments(status)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_punishments_type
                ON punishments(punishment_type)
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
                CREATE INDEX IF NOT EXISTS idx_punishments_minecraft_name
                ON punishments(minecraft_name)
                """);

        statements.add("""
                CREATE INDEX IF NOT EXISTS idx_punishments_discord_id
                ON punishments(discord_id)
                """);

        return statements;
    }

    private List<String> getMySqlV2Statements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS punishments (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    discord_name VARCHAR(100) NULL,
                    discord_id VARCHAR(64) NULL,
                    minecraft_name VARCHAR(16) NOT NULL,
                    minecraft_uuid VARCHAR(36) NULL,
                    punishment_type VARCHAR(32) NOT NULL,
                    reason TEXT NULL,
                    issued_by_name VARCHAR(100) NULL,
                    issued_by_uuid VARCHAR(36) NULL,
                    issued_by_discord_name VARCHAR(100) NULL,
                    issued_by_discord_id VARCHAR(64) NULL,
                    issued_source VARCHAR(32) NULL,
                    duration_ms BIGINT NULL,
                    issued_at DATETIME(3) NOT NULL,
                    expires_at DATETIME(3) NULL,
                    status VARCHAR(32) NOT NULL,
                    removed_at DATETIME(3) NULL,
                    removed_by_name VARCHAR(100) NULL,
                    removed_by_uuid VARCHAR(36) NULL,
                    removed_by_discord_name VARCHAR(100) NULL,
                    removed_by_discord_id VARCHAR(64) NULL,
                    remove_reason TEXT NULL,
                    server_name VARCHAR(100) NULL,
                    notify_on_join BOOLEAN NOT NULL DEFAULT FALSE,
                    notification_delivered BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (id),
                    INDEX idx_punishments_minecraft_uuid (minecraft_uuid),
                    INDEX idx_punishments_status (status),
                    INDEX idx_punishments_type (punishment_type),
                    INDEX idx_punishments_type_status (punishment_type, status),
                    INDEX idx_punishments_expires_at (expires_at),
                    INDEX idx_punishments_minecraft_name (minecraft_name),
                    INDEX idx_punishments_discord_id (discord_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        return statements;
    }

    private List<String> getSqliteV3Statements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS pending_changes (
                    minecraft_name TEXT NOT NULL,
                    minecraft_uuid TEXT NOT NULL UNIQUE,
                    discord_name TEXT,
                    discord_id TEXT,
                    inventory_data TEXT,
                    enderchest_data TEXT,
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

        return statements;
    }

    private List<String> getMySqlV3Statements() {
        List<String> statements = new ArrayList<>();

        statements.add("""
                CREATE TABLE IF NOT EXISTS pending_changes (
                    minecraft_name VARCHAR(16) NOT NULL,
                    minecraft_uuid VARCHAR(36) NOT NULL,
                    discord_name VARCHAR(100) NULL,
                    discord_id VARCHAR(64) NULL,
                    inventory_data LONGTEXT NULL,
                    enderchest_data LONGTEXT NULL,
                    last_online DATETIME(3) NULL,
                    last_edited DATETIME(3) NULL,
                    last_applied DATETIME(3) NULL,
                    edited_by VARCHAR(100) NULL,
                    has_pending_changes BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (minecraft_uuid),
                    INDEX idx_pending_changes_minecraft_name (minecraft_name),
                    INDEX idx_pending_changes_discord_id (discord_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        return statements;
    }

    private void ensureWhitelistEntriesColumns() {
        String databaseType = normalizeDatabaseType();

        if (databaseType.equals("MYSQL")) {
            ensureMysqlWhitelistEntriesReviewerNameColumn();
            ensureMysqlWhitelistEntriesDateTimeColumn("submitted_at", false);
            ensureMysqlWhitelistEntriesDateTimeColumn("reviewed_at", true);
            ensureMysqlWhitelistEntriesDateTimeColumn("linked_at", true);
            ensureMysqlWhitelistEntriesDateTimeColumn("finalized_at", true);
        } else {
            ensureSqliteWhitelistEntriesColumn("reviewed_by_discord_name", "TEXT");
        }
    }

    private void ensureWhitelistEntriesNonUniqueConstraints() {
        String databaseType = normalizeDatabaseType();

        if (databaseType.equals("MYSQL")) {
            ensureMysqlWhitelistEntriesNonUniqueConstraints();
        } else {
            ensureSqliteWhitelistEntriesNonUniqueConstraints();
        }
    }

    private void ensureMysqlWhitelistEntriesReviewerNameColumn() {
        if (mysqlColumnExists("whitelist_entries", "reviewed_by_discord_name")) {
            return;
        }

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE whitelist_entries ADD COLUMN reviewed_by_discord_name VARCHAR(100) NULL AFTER reviewed_by_discord_id");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to ensure MySQL whitelist_entries reviewer name column", ex);
        }
    }

    private void ensureMysqlWhitelistEntriesDateTimeColumn(String columnName, boolean nullable) {
        String currentType = getMysqlColumnType("whitelist_entries", columnName);
        if (isDateTimeType(currentType)) {
            return;
        }

        String tempColumn = columnName + "_dt_tmp";

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {

            if (!mysqlColumnExists("whitelist_entries", tempColumn)) {
                statement.executeUpdate("ALTER TABLE whitelist_entries ADD COLUMN " + tempColumn + " DATETIME(3) NULL");
            }

            statement.executeUpdate("""
                    UPDATE whitelist_entries
                    SET %s = CASE
                        WHEN %s IS NULL THEN NULL
                        ELSE FROM_UNIXTIME(%s / 1000.0)
                    END
                    """.formatted(tempColumn, columnName, columnName));

            statement.executeUpdate("ALTER TABLE whitelist_entries DROP COLUMN " + columnName);

            String nullSql = nullable ? "NULL" : "NOT NULL";
            statement.executeUpdate("ALTER TABLE whitelist_entries CHANGE COLUMN " + tempColumn + " " + columnName + " DATETIME(3) " + nullSql);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert MySQL whitelist_entries column to DATETIME: " + columnName, ex);
        }
    }

    private void ensureSqliteWhitelistEntriesColumn(String columnName, String definition) {
        String sql = "PRAGMA table_info(whitelist_entries)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String existing = rs.getString("name");
                if (columnName.equalsIgnoreCase(existing)) {
                    return;
                }
            }

            try (Statement alter = connection.createStatement()) {
                alter.executeUpdate("ALTER TABLE whitelist_entries ADD COLUMN " + columnName + " " + definition);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to ensure SQLite whitelist_entries column: " + columnName, ex);
        }
    }

    private void ensureSqliteWhitelistEntriesNonUniqueConstraints() {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA foreign_keys = OFF");

                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS whitelist_entries_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            application_id TEXT NOT NULL UNIQUE,
                            discord_user_id TEXT NOT NULL,
                            discord_tag TEXT NOT NULL,
                            minecraft_name TEXT NOT NULL,
                            minecraft_uuid TEXT,
                            status TEXT NOT NULL,
                            review_reason TEXT,
                            reviewed_by_discord_id TEXT,
                            reviewed_by_discord_name TEXT,
                            submitted_at INTEGER NOT NULL,
                            reviewed_at INTEGER,
                            linked_at INTEGER,
                            finalized_at INTEGER,
                            name_retry_count INTEGER NOT NULL DEFAULT 0,
                            form_answers_json TEXT NOT NULL
                        )
                        """);

                statement.executeUpdate("""
                        INSERT INTO whitelist_entries_new (
                            id,
                            application_id,
                            discord_user_id,
                            discord_tag,
                            minecraft_name,
                            minecraft_uuid,
                            status,
                            review_reason,
                            reviewed_by_discord_id,
                            reviewed_by_discord_name,
                            submitted_at,
                            reviewed_at,
                            linked_at,
                            finalized_at,
                            name_retry_count,
                            form_answers_json
                        )
                        SELECT
                            id,
                            application_id,
                            discord_user_id,
                            discord_tag,
                            minecraft_name,
                            minecraft_uuid,
                            status,
                            review_reason,
                            reviewed_by_discord_id,
                            reviewed_by_discord_name,
                            submitted_at,
                            reviewed_at,
                            linked_at,
                            finalized_at,
                            name_retry_count,
                            form_answers_json
                        FROM whitelist_entries
                        """);

                statement.executeUpdate("DROP TABLE whitelist_entries");
                statement.executeUpdate("ALTER TABLE whitelist_entries_new RENAME TO whitelist_entries");

                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_whitelist_entries_status
                        ON whitelist_entries(status)
                        """);

                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_whitelist_entries_discord_user_id
                        ON whitelist_entries(discord_user_id)
                        """);

                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_whitelist_entries_minecraft_name
                        ON whitelist_entries(minecraft_name)
                        """);

                statement.executeUpdate("PRAGMA foreign_keys = ON");

                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw new RuntimeException("Failed to rebuild SQLite whitelist_entries without unique user/name constraints", ex);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to migrate SQLite whitelist_entries constraints", ex);
        }
    }

    private void ensureMysqlWhitelistEntriesNonUniqueConstraints() {
        ensureMysqlUniqueIndexDropped("whitelist_entries", "uk_whitelist_entries_discord_user_id");
        ensureMysqlUniqueIndexDropped("whitelist_entries", "uk_whitelist_entries_minecraft_name");
        ensureMysqlIndex("whitelist_entries", "idx_whitelist_entries_discord_user_id", "CREATE INDEX idx_whitelist_entries_discord_user_id ON whitelist_entries(discord_user_id)");
        ensureMysqlIndex("whitelist_entries", "idx_whitelist_entries_minecraft_name", "CREATE INDEX idx_whitelist_entries_minecraft_name ON whitelist_entries(minecraft_name)");
    }

    private void ensurePunishmentsColumns() {
        String databaseType = normalizeDatabaseType();

        if (databaseType.equals("MYSQL")) {
            ensureMysqlPunishmentsDateTimeColumn("issued_at", false);
            ensureMysqlPunishmentsDateTimeColumn("expires_at", true);
            ensureMysqlPunishmentsDateTimeColumn("removed_at", true);
            ensureMysqlPunishmentsColumn("notify_on_join", "BOOLEAN NOT NULL DEFAULT FALSE");
            ensureMysqlPunishmentsColumn("notification_delivered", "BOOLEAN NOT NULL DEFAULT FALSE");
        } else {
            ensureSqlitePunishmentsDateTimeColumns();
            ensureSqlitePunishmentsColumn("notify_on_join", "INTEGER NOT NULL DEFAULT 0");
            ensureSqlitePunishmentsColumn("notification_delivered", "INTEGER NOT NULL DEFAULT 0");
        }
    }

    private void ensurePunishmentsIndexes() {
        String databaseType = normalizeDatabaseType();

        if (databaseType.equals("MYSQL")) {
            ensureMysqlIndex("punishments", "idx_punishments_minecraft_uuid", "CREATE INDEX idx_punishments_minecraft_uuid ON punishments(minecraft_uuid)");
            ensureMysqlIndex("punishments", "idx_punishments_status", "CREATE INDEX idx_punishments_status ON punishments(status)");
            ensureMysqlIndex("punishments", "idx_punishments_type", "CREATE INDEX idx_punishments_type ON punishments(punishment_type)");
            ensureMysqlIndex("punishments", "idx_punishments_type_status", "CREATE INDEX idx_punishments_type_status ON punishments(punishment_type, status)");
            ensureMysqlIndex("punishments", "idx_punishments_expires_at", "CREATE INDEX idx_punishments_expires_at ON punishments(expires_at)");
            ensureMysqlIndex("punishments", "idx_punishments_minecraft_name", "CREATE INDEX idx_punishments_minecraft_name ON punishments(minecraft_name)");
            ensureMysqlIndex("punishments", "idx_punishments_discord_id", "CREATE INDEX idx_punishments_discord_id ON punishments(discord_id)");
        } else {
            executeSimple("""
                    CREATE INDEX IF NOT EXISTS idx_punishments_minecraft_uuid
                    ON punishments(minecraft_uuid)
                    """);
            executeSimple("""
                    CREATE INDEX IF NOT EXISTS idx_punishments_status
                    ON punishments(status)
                    """);
            executeSimple("""
                    CREATE INDEX IF NOT EXISTS idx_punishments_type
                    ON punishments(punishment_type)
                    """);
            executeSimple("""
                    CREATE INDEX IF NOT EXISTS idx_punishments_type_status
                    ON punishments(punishment_type, status)
                    """);
            executeSimple("""
                    CREATE INDEX IF NOT EXISTS idx_punishments_expires_at
                    ON punishments(expires_at)
                    """);
            executeSimple("""
                    CREATE INDEX IF NOT EXISTS idx_punishments_minecraft_name
                    ON punishments(minecraft_name)
                    """);
            executeSimple("""
                    CREATE INDEX IF NOT EXISTS idx_punishments_discord_id
                    ON punishments(discord_id)
                    """);
        }
    }

    private void ensurePendingChangesColumns() {
        String databaseType = normalizeDatabaseType();

        if (databaseType.equals("MYSQL")) {
            ensureMysqlPendingChangesColumn("minecraft_name", "VARCHAR(16) NOT NULL");
            ensureMysqlPendingChangesColumn("discord_name", "VARCHAR(100) NULL");
            ensureMysqlPendingChangesColumn("discord_id", "VARCHAR(64) NULL");
            ensureMysqlPendingChangesColumn("inventory_data", "LONGTEXT NULL");
            ensureMysqlPendingChangesColumn("enderchest_data", "LONGTEXT NULL");
            ensureMysqlPendingChangesColumn("last_online", "DATETIME(3) NULL");
            ensureMysqlPendingChangesColumn("last_edited", "DATETIME(3) NULL");
            ensureMysqlPendingChangesColumn("last_applied", "DATETIME(3) NULL");
            ensureMysqlPendingChangesColumn("edited_by", "VARCHAR(100) NULL");
            ensureMysqlPendingChangesColumn("has_pending_changes", "BOOLEAN NOT NULL DEFAULT FALSE");
        } else {
            ensureSqlitePendingChangesColumn("minecraft_name", "TEXT NOT NULL");
            ensureSqlitePendingChangesColumn("discord_name", "TEXT");
            ensureSqlitePendingChangesColumn("discord_id", "TEXT");
            ensureSqlitePendingChangesColumn("inventory_data", "TEXT");
            ensureSqlitePendingChangesColumn("enderchest_data", "TEXT");
            ensureSqlitePendingChangesColumn("last_online", "TEXT");
            ensureSqlitePendingChangesColumn("last_edited", "TEXT");
            ensureSqlitePendingChangesColumn("last_applied", "TEXT");
            ensureSqlitePendingChangesColumn("edited_by", "TEXT");
            ensureSqlitePendingChangesColumn("has_pending_changes", "INTEGER NOT NULL DEFAULT 0");
        }
    }

    private void ensurePendingChangesIndexes() {
        String databaseType = normalizeDatabaseType();

        if (databaseType.equals("MYSQL")) {
            ensureMysqlIndex("pending_changes", "idx_pending_changes_minecraft_name", "CREATE INDEX idx_pending_changes_minecraft_name ON pending_changes(minecraft_name)");
            ensureMysqlIndex("pending_changes", "idx_pending_changes_discord_id", "CREATE INDEX idx_pending_changes_discord_id ON pending_changes(discord_id)");
        } else {
            executeSimple("""
                    CREATE INDEX IF NOT EXISTS idx_pending_changes_minecraft_name
                    ON pending_changes(minecraft_name)
                    """);
            executeSimple("""
                    CREATE INDEX IF NOT EXISTS idx_pending_changes_discord_id
                    ON pending_changes(discord_id)
                    """);
        }
    }

    private void ensureMysqlPunishmentsDateTimeColumn(String columnName, boolean nullable) {
        String currentType = getMysqlColumnType("punishments", columnName);
        if (isDateTimeType(currentType)) {
            return;
        }

        String tempColumn = columnName + "_dt_tmp";

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {

            if (!mysqlColumnExists("punishments", tempColumn)) {
                statement.executeUpdate("ALTER TABLE punishments ADD COLUMN " + tempColumn + " DATETIME(3) NULL");
            }

            statement.executeUpdate("""
                    UPDATE punishments
                    SET %s = CASE
                        WHEN %s IS NULL THEN NULL
                        ELSE FROM_UNIXTIME(%s / 1000.0)
                    END
                    """.formatted(tempColumn, columnName, columnName));

            statement.executeUpdate("ALTER TABLE punishments DROP COLUMN " + columnName);

            String nullSql = nullable ? "NULL" : "NOT NULL";
            statement.executeUpdate("ALTER TABLE punishments CHANGE COLUMN " + tempColumn + " " + columnName + " DATETIME(3) " + nullSql);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert MySQL punishments column to DATETIME: " + columnName, ex);
        }
    }

    private void ensureSqlitePunishmentsDateTimeColumns() {
        if (sqlitePunishmentsDateColumnsAreText()) {
            return;
        }

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA foreign_keys = OFF");

                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS punishments_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            discord_name TEXT,
                            discord_id TEXT,
                            minecraft_name TEXT NOT NULL,
                            minecraft_uuid TEXT,
                            punishment_type TEXT NOT NULL,
                            reason TEXT,
                            issued_by_name TEXT,
                            issued_by_uuid TEXT,
                            issued_by_discord_name TEXT,
                            issued_by_discord_id TEXT,
                            issued_source TEXT,
                            duration_ms INTEGER,
                            issued_at TEXT NOT NULL,
                            expires_at TEXT,
                            status TEXT NOT NULL,
                            removed_at TEXT,
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

                statement.executeUpdate("""
                        INSERT INTO punishments_new (
                            id,
                            discord_name,
                            discord_id,
                            minecraft_name,
                            minecraft_uuid,
                            punishment_type,
                            reason,
                            issued_by_name,
                            issued_by_uuid,
                            issued_by_discord_name,
                            issued_by_discord_id,
                            issued_source,
                            duration_ms,
                            issued_at,
                            expires_at,
                            status,
                            removed_at,
                            removed_by_name,
                            removed_by_uuid,
                            removed_by_discord_name,
                            removed_by_discord_id,
                            remove_reason,
                            server_name,
                            notify_on_join,
                            notification_delivered
                        )
                        SELECT
                            id,
                            discord_name,
                            discord_id,
                            minecraft_name,
                            minecraft_uuid,
                            punishment_type,
                            reason,
                            issued_by_name,
                            issued_by_uuid,
                            issued_by_discord_name,
                            issued_by_discord_id,
                            issued_source,
                            duration_ms,
                            CASE
                                WHEN issued_at IS NULL THEN NULL
                                WHEN typeof(issued_at) IN ('integer', 'real')
                                    THEN strftime('%Y-%m-%d %H:%M:%f', issued_at / 1000.0, 'unixepoch')
                                ELSE issued_at
                            END,
                            CASE
                                WHEN expires_at IS NULL THEN NULL
                                WHEN typeof(expires_at) IN ('integer', 'real')
                                    THEN strftime('%Y-%m-%d %H:%M:%f', expires_at / 1000.0, 'unixepoch')
                                ELSE expires_at
                            END,
                            status,
                            CASE
                                WHEN removed_at IS NULL THEN NULL
                                WHEN typeof(removed_at) IN ('integer', 'real')
                                    THEN strftime('%Y-%m-%d %H:%M:%f', removed_at / 1000.0, 'unixepoch')
                                ELSE removed_at
                            END,
                            removed_by_name,
                            removed_by_uuid,
                            removed_by_discord_name,
                            removed_by_discord_id,
                            remove_reason,
                            server_name,
                            notify_on_join,
                            notification_delivered
                        FROM punishments
                        """);

                statement.executeUpdate("DROP TABLE punishments");
                statement.executeUpdate("ALTER TABLE punishments_new RENAME TO punishments");

                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_punishments_minecraft_uuid
                        ON punishments(minecraft_uuid)
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_punishments_status
                        ON punishments(status)
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_punishments_type
                        ON punishments(punishment_type)
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_punishments_type_status
                        ON punishments(punishment_type, status)
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_punishments_expires_at
                        ON punishments(expires_at)
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_punishments_minecraft_name
                        ON punishments(minecraft_name)
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_punishments_discord_id
                        ON punishments(discord_id)
                        """);

                statement.executeUpdate("PRAGMA foreign_keys = ON");
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw new RuntimeException("Failed to rebuild SQLite punishments table with datetime text columns", ex);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to migrate SQLite punishments datetime columns", ex);
        }
    }

    private boolean sqlitePunishmentsDateColumnsAreText() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(punishments)");
             ResultSet rs = statement.executeQuery()) {

            String issuedType = null;
            String expiresType = null;
            String removedType = null;

            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");

                if ("issued_at".equalsIgnoreCase(name)) {
                    issuedType = type;
                } else if ("expires_at".equalsIgnoreCase(name)) {
                    expiresType = type;
                } else if ("removed_at".equalsIgnoreCase(name)) {
                    removedType = type;
                }
            }

            return isTextType(issuedType) && isTextType(expiresType) && isTextType(removedType);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to inspect SQLite punishments datetime column types", ex);
        }
    }

    private boolean isTextType(String type) {
        return type != null && type.trim().toUpperCase(Locale.ROOT).contains("TEXT");
    }

    private void ensureMysqlPunishmentsColumn(String columnName, String definition) {
        String checkSql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'punishments'
                  AND COLUMN_NAME = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement check = connection.prepareStatement(checkSql)) {

            check.setString(1, columnName);

            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE punishments ADD COLUMN " + columnName + " " + definition);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to ensure MySQL punishments column: " + columnName, ex);
        }
    }

    private void ensureSqlitePunishmentsColumn(String columnName, String definition) {
        String sql = "PRAGMA table_info(punishments)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String existing = rs.getString("name");
                if (columnName.equalsIgnoreCase(existing)) {
                    return;
                }
            }

            try (Statement alter = connection.createStatement()) {
                alter.executeUpdate("ALTER TABLE punishments ADD COLUMN " + columnName + " " + definition);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to ensure SQLite punishments column: " + columnName, ex);
        }
    }

    private void ensureMysqlPendingChangesColumn(String columnName, String definition) {
        String checkSql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'pending_changes'
                  AND COLUMN_NAME = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement check = connection.prepareStatement(checkSql)) {

            check.setString(1, columnName);

            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE pending_changes ADD COLUMN " + columnName + " " + definition);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to ensure MySQL pending_changes column: " + columnName, ex);
        }
    }

    private void ensureSqlitePendingChangesColumn(String columnName, String definition) {
        String sql = "PRAGMA table_info(pending_changes)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String existing = rs.getString("name");
                if (columnName.equalsIgnoreCase(existing)) {
                    return;
                }
            }

            try (Statement alter = connection.createStatement()) {
                alter.executeUpdate("ALTER TABLE pending_changes ADD COLUMN " + columnName + " " + definition);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to ensure SQLite pending_changes column: " + columnName, ex);
        }
    }

    private void ensureMysqlUniqueIndexDropped(String tableName, String indexName) {
        String checkSql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement check = connection.prepareStatement(checkSql)) {

            check.setString(1, tableName);
            check.setString(2, indexName);

            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next() || rs.getInt(1) <= 0) {
                    return;
                }
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE " + tableName + " DROP INDEX " + indexName);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to drop MySQL unique index: " + indexName, ex);
        }
    }

    private boolean mysqlColumnExists(String tableName, String columnName) {
        String checkSql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement check = connection.prepareStatement(checkSql)) {

            check.setString(1, tableName);
            check.setString(2, columnName);

            try (ResultSet rs = check.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to inspect MySQL column: " + tableName + "." + columnName, ex);
        }
    }

    private String getMysqlColumnType(String tableName, String columnName) {
        String checkSql = """
                SELECT DATA_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement check = connection.prepareStatement(checkSql)) {

            check.setString(1, tableName);
            check.setString(2, columnName);

            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to inspect MySQL column type: " + tableName + "." + columnName, ex);
        }
    }

    private void ensureMysqlIndex(String tableName, String indexName, String createSql) {
        String checkSql = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement check = connection.prepareStatement(checkSql)) {

            check.setString(1, tableName);
            check.setString(2, indexName);

            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createSql);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to ensure MySQL index: " + indexName, ex);
        }
    }

    private void executeSimple(String sql) {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to execute SQL: " + sql, ex);
        }
    }

    private String normalizeDatabaseType() {
        String type = config.getDatabaseType();
        if (type == null) {
            return "SQLITE";
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private record Migration(String version, String description, List<String> statements) {
    }
}