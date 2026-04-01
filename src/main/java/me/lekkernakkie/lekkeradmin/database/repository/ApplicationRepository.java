package me.lekkernakkie.lekkeradmin.database.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.DatabaseManager;
import me.lekkernakkie.lekkeradmin.model.application.ApplicationAnswer;
import me.lekkernakkie.lekkeradmin.model.application.ApplicationStatus;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationRepository {

    private static final Type ANSWER_LIST_TYPE = new TypeToken<List<ApplicationAnswer>>() {}.getType();

    private final DatabaseManager databaseManager;
    private final Gson gson = new Gson();
    private final Map<String, Boolean> dateTimeColumnCache = new ConcurrentHashMap<>();

    public ApplicationRepository(LekkerAdmin plugin) {
        this.databaseManager = plugin.getDatabaseManager();
    }

    public void saveApplication(WhitelistApplication application) {
        String sql = """
                INSERT INTO whitelist_entries (
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
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, application.getApplicationId());
            statement.setString(2, application.getDiscordUserId());
            statement.setString(3, application.getDiscordTag());
            statement.setString(4, application.getMinecraftName());
            statement.setString(5, application.getMinecraftUuid());
            statement.setString(6, application.getStatus().name());
            statement.setString(7, application.getReviewReason());
            statement.setString(8, application.getReviewedByDiscordId());
            statement.setString(9, application.getReviewedByDiscordName());
            setTemporal(statement, connection, "whitelist_entries", "submitted_at", 10, application.getSubmittedAt(), false);
            setTemporal(statement, connection, "whitelist_entries", "reviewed_at", 11, application.getReviewedAt(), true);
            setTemporal(statement, connection, "whitelist_entries", "linked_at", 12, application.getLinkedAt(), true);
            setTemporal(statement, connection, "whitelist_entries", "finalized_at", 13, application.getFinalizedAt(), true);
            statement.setInt(14, application.getNameRetryCount());
            statement.setString(15, toJson(application.getAnswers()));

            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save whitelist application: " + application.getApplicationId(), ex);
        }
    }

    public void updateApplication(WhitelistApplication application) {
        String sql = """
                UPDATE whitelist_entries
                SET discord_tag = ?,
                    minecraft_name = ?,
                    minecraft_uuid = ?,
                    status = ?,
                    review_reason = ?,
                    reviewed_by_discord_id = ?,
                    reviewed_by_discord_name = ?,
                    submitted_at = ?,
                    reviewed_at = ?,
                    linked_at = ?,
                    finalized_at = ?,
                    name_retry_count = ?,
                    form_answers_json = ?
                WHERE application_id = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, application.getDiscordTag());
            statement.setString(2, application.getMinecraftName());
            statement.setString(3, application.getMinecraftUuid());
            statement.setString(4, application.getStatus().name());
            statement.setString(5, application.getReviewReason());
            statement.setString(6, application.getReviewedByDiscordId());
            statement.setString(7, application.getReviewedByDiscordName());
            setTemporal(statement, connection, "whitelist_entries", "submitted_at", 8, application.getSubmittedAt(), false);
            setTemporal(statement, connection, "whitelist_entries", "reviewed_at", 9, application.getReviewedAt(), true);
            setTemporal(statement, connection, "whitelist_entries", "linked_at", 10, application.getLinkedAt(), true);
            setTemporal(statement, connection, "whitelist_entries", "finalized_at", 11, application.getFinalizedAt(), true);
            statement.setInt(12, application.getNameRetryCount());
            statement.setString(13, toJson(application.getAnswers()));
            statement.setString(14, application.getApplicationId());

            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to update whitelist application: " + application.getApplicationId(), ex);
        }
    }

    public Optional<WhitelistApplication> findByApplicationId(String applicationId) {
        String sql = "SELECT * FROM whitelist_entries WHERE application_id = ? LIMIT 1";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, applicationId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapApplication(rs));
                }
                return Optional.empty();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load application: " + applicationId, ex);
        }
    }

    public Optional<WhitelistApplication> findLatestByDiscordUserId(String discordUserId) {
        String sql = """
                SELECT * FROM whitelist_entries
                WHERE discord_user_id = ?
                ORDER BY submitted_at DESC
                LIMIT 1
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, discordUserId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapApplication(rs));
                }
                return Optional.empty();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load latest application for discord user: " + discordUserId, ex);
        }
    }

    public Optional<WhitelistApplication> findLatestOpenByDiscordUserId(String discordUserId) {
        String sql = """
                SELECT * FROM whitelist_entries
                WHERE discord_user_id = ?
                  AND status IN (?, ?, ?)
                ORDER BY submitted_at DESC
                LIMIT 1
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, discordUserId);
            statement.setString(2, ApplicationStatus.PENDING_REVIEW.name());
            statement.setString(3, ApplicationStatus.APPROVED_PENDING_NAME_FIX.name());
            statement.setString(4, ApplicationStatus.APPROVED.name());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapApplication(rs));
                }
                return Optional.empty();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load open application for discord user: " + discordUserId, ex);
        }
    }

    public Optional<WhitelistApplication> findByDiscordUserId(String discordUserId) {
        String sql = """
                SELECT * FROM whitelist_entries
                WHERE discord_user_id = ?
                ORDER BY
                    CASE WHEN linked_at IS NULL THEN 1 ELSE 0 END,
                    linked_at DESC,
                    submitted_at DESC
                LIMIT 1
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, discordUserId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapApplication(rs));
                }
                return Optional.empty();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load application by discord user id: " + discordUserId, ex);
        }
    }

    public Optional<WhitelistApplication> findByMinecraftName(String minecraftName) {
        String sql = """
                SELECT * FROM whitelist_entries
                WHERE minecraft_name = ?
                ORDER BY
                    CASE WHEN linked_at IS NULL THEN 1 ELSE 0 END,
                    linked_at DESC,
                    submitted_at DESC
                LIMIT 1
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, minecraftName);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapApplication(rs));
                }
                return Optional.empty();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load application by minecraft name: " + minecraftName, ex);
        }
    }

    private WhitelistApplication mapApplication(ResultSet rs) throws Exception {
        Connection connection = rs.getStatement().getConnection();

        WhitelistApplication application = new WhitelistApplication();
        application.setId(rs.getLong("id"));
        application.setApplicationId(rs.getString("application_id"));
        application.setDiscordUserId(rs.getString("discord_user_id"));
        application.setDiscordTag(rs.getString("discord_tag"));
        application.setMinecraftName(rs.getString("minecraft_name"));
        application.setMinecraftUuid(rs.getString("minecraft_uuid"));
        application.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        application.setReviewReason(rs.getString("review_reason"));
        application.setReviewedByDiscordId(rs.getString("reviewed_by_discord_id"));
        application.setReviewedByDiscordName(rs.getString("reviewed_by_discord_name"));
        application.setSubmittedAt(getRequiredTemporal(rs, connection, "whitelist_entries", "submitted_at"));
        application.setReviewedAt(getNullableTemporal(rs, connection, "whitelist_entries", "reviewed_at"));
        application.setLinkedAt(getNullableTemporal(rs, connection, "whitelist_entries", "linked_at"));
        application.setFinalizedAt(getNullableTemporal(rs, connection, "whitelist_entries", "finalized_at"));
        application.setNameRetryCount(rs.getInt("name_retry_count"));
        application.setFormAnswersJson(rs.getString("form_answers_json"));
        application.setAnswers(fromJson(rs.getString("form_answers_json")));
        return application;
    }

    private String toJson(List<ApplicationAnswer> answers) {
        return gson.toJson(answers == null ? new ArrayList<>() : answers, ANSWER_LIST_TYPE);
    }

    private List<ApplicationAnswer> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        List<ApplicationAnswer> answers = gson.fromJson(json, ANSWER_LIST_TYPE);
        return answers == null ? new ArrayList<>() : answers;
    }

    private void setTemporal(PreparedStatement statement,
                             Connection connection,
                             String tableName,
                             String columnName,
                             int index,
                             Long value,
                             boolean nullable) throws Exception {

        if (value == null) {
            if (nullable) {
                if (isDateTimeColumn(connection, tableName, columnName)) {
                    statement.setNull(index, java.sql.Types.TIMESTAMP);
                } else {
                    statement.setNull(index, java.sql.Types.BIGINT);
                }
                return;
            }
            throw new IllegalArgumentException("Non-null temporal column received null value: " + columnName);
        }

        if (isDateTimeColumn(connection, tableName, columnName)) {
            statement.setTimestamp(index, new Timestamp(value));
        } else {
            statement.setLong(index, value);
        }
    }

    private long getRequiredTemporal(ResultSet rs,
                                     Connection connection,
                                     String tableName,
                                     String columnName) throws Exception {
        Long value = getNullableTemporal(rs, connection, tableName, columnName);
        if (value == null) {
            throw new IllegalStateException("Required temporal column is null: " + columnName);
        }
        return value;
    }

    private Long getNullableTemporal(ResultSet rs,
                                     Connection connection,
                                     String tableName,
                                     String columnName) throws Exception {
        if (isDateTimeColumn(connection, tableName, columnName)) {
            Timestamp timestamp = rs.getTimestamp(columnName);
            return timestamp == null ? null : timestamp.getTime();
        }

        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private boolean isDateTimeColumn(Connection connection, String tableName, String columnName) {
        String cacheKey = tableName + "." + columnName;
        Boolean cached = dateTimeColumnCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            while (columns.next()) {
                String typeName = columns.getString("TYPE_NAME");
                if (typeName == null) {
                    continue;
                }

                String normalized = typeName.trim().toLowerCase(Locale.ROOT);
                boolean isDateTime = normalized.contains("datetime")
                        || normalized.contains("timestamp")
                        || normalized.equals("date");

                dateTimeColumnCache.put(cacheKey, isDateTime);
                return isDateTime;
            }
        } catch (Exception ignored) {
        }

        dateTimeColumnCache.put(cacheKey, false);
        return false;
    }
}