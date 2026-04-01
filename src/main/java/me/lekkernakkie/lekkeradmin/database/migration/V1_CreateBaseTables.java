package me.lekkernakkie.lekkeradmin.database.migration;

public class V1_CreateBaseTables {

    public String version() {
        return "V1";
    }

    public String description() {
        return "Create base whitelist, link and audit tables";
    }

    public String sql() {
        return """
                CREATE TABLE IF NOT EXISTS whitelist_applications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    application_id VARCHAR(64) NOT NULL UNIQUE,
                    discord_user_id VARCHAR(32) NOT NULL,
                    discord_tag VARCHAR(100),
                    minecraft_name VARCHAR(16),
                    minecraft_uuid VARCHAR(36),
                    status VARCHAR(50) NOT NULL,
                    submitted_at BIGINT NOT NULL,
                    reviewed_at BIGINT,
                    reviewed_by_discord_id VARCHAR(32),
                    review_reason TEXT,
                    name_retry_count INTEGER NOT NULL DEFAULT 0,
                    finalized_at BIGINT
                );

                CREATE TABLE IF NOT EXISTS whitelist_application_answers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    application_id VARCHAR(64) NOT NULL,
                    field_key VARCHAR(100) NOT NULL,
                    field_label VARCHAR(255) NOT NULL,
                    field_value TEXT,
                    field_order INTEGER NOT NULL DEFAULT 0
                );

                CREATE TABLE IF NOT EXISTS discord_minecraft_links (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    discord_user_id VARCHAR(32) NOT NULL UNIQUE,
                    discord_tag VARCHAR(100),
                    minecraft_uuid VARCHAR(36) NOT NULL,
                    minecraft_name VARCHAR(16) NOT NULL,
                    linked_at BIGINT NOT NULL,
                    linked_by_application_id VARCHAR(64)
                );

                CREATE TABLE IF NOT EXISTS audit_log_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action_type VARCHAR(100) NOT NULL,
                    actor_discord_id VARCHAR(32),
                    actor_name VARCHAR(100),
                    target_discord_id VARCHAR(32),
                    target_minecraft_name VARCHAR(16),
                    application_id VARCHAR(64),
                    details TEXT,
                    created_at BIGINT NOT NULL
                );

                CREATE INDEX IF NOT EXISTS idx_whitelist_applications_discord_user_id
                ON whitelist_applications(discord_user_id);

                CREATE INDEX IF NOT EXISTS idx_whitelist_applications_status
                ON whitelist_applications(status);

                CREATE INDEX IF NOT EXISTS idx_whitelist_application_answers_application_id
                ON whitelist_application_answers(application_id);

                CREATE INDEX IF NOT EXISTS idx_discord_minecraft_links_minecraft_name
                ON discord_minecraft_links(minecraft_name);

                CREATE INDEX IF NOT EXISTS idx_audit_log_entries_application_id
                ON audit_log_entries(application_id);
                """;
    }
}