package me.lekkernakkie.lekkeradmin.database.repository;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.DatabaseManager;
import me.lekkernakkie.lekkeradmin.model.audit.AuditLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AuditLogRepository {

    private final DatabaseManager databaseManager;

    public AuditLogRepository(LekkerAdmin plugin) {
        this.databaseManager = plugin.getDatabaseManager();
    }

    public void save(AuditLogEntry entry) {
        String sql = """
                INSERT INTO audit_log_entries (
                    action_type,
                    actor_id,
                    actor_name,
                    target_id,
                    target_name,
                    application_id,
                    details,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, entry.getActionType());
            statement.setString(2, entry.getActorId());
            statement.setString(3, entry.getActorName());
            statement.setString(4, entry.getTargetId());
            statement.setString(5, entry.getTargetName());
            statement.setString(6, entry.getApplicationId());
            statement.setString(7, entry.getDetails());
            statement.setLong(8, entry.getCreatedAt());

            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save audit log entry.", ex);
        }
    }
}