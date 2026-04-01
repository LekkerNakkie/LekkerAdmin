package me.lekkernakkie.lekkeradmin.database.repository;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentSource;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentStatus;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PunishmentRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String HISTORY_CLEAR_PREFIX = "[HISTORY_CLEAR]%";

    private final LekkerAdmin plugin;

    public PunishmentRepository(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void createPunishment(PunishmentEntry entry) {
        String sql = """
                INSERT INTO punishments
                (
                    discord_name, discord_id, minecraft_name, minecraft_uuid,
                    punishment_type, reason,
                    issued_by_name, issued_by_uuid, issued_by_discord_name, issued_by_discord_id,
                    issued_source, duration_ms, issued_at, expires_at, status,
                    removed_at, removed_by_name, removed_by_uuid, removed_by_discord_name, removed_by_discord_id,
                    remove_reason, server_name, notify_on_join, notification_delivered
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, entry.getDiscordName());
            statement.setString(2, entry.getDiscordId());
            statement.setString(3, entry.getMinecraftName());
            statement.setString(4, entry.getMinecraftUuid());
            statement.setString(5, entry.getPunishmentType() == null ? null : entry.getPunishmentType().name());
            statement.setString(6, entry.getReason());
            statement.setString(7, entry.getIssuedByName());
            statement.setString(8, entry.getIssuedByUuid());
            statement.setString(9, entry.getIssuedByDiscordName());
            statement.setString(10, entry.getIssuedByDiscordId());
            statement.setString(11, entry.getIssuedSource() == null ? null : entry.getIssuedSource().name());
            setNullableLong(statement, 12, entry.getDurationMs());
            setDateTime(statement, 13, entry.getIssuedAt());
            setNullableDateTime(statement, 14, entry.getExpiresAt());
            statement.setString(15, entry.getStatus() == null ? null : entry.getStatus().name());
            setNullableDateTime(statement, 16, entry.getRemovedAt());
            statement.setString(17, entry.getRemovedByName());
            statement.setString(18, entry.getRemovedByUuid());
            statement.setString(19, entry.getRemovedByDiscordName());
            statement.setString(20, entry.getRemovedByDiscordId());
            statement.setString(21, entry.getRemoveReason());
            statement.setString(22, entry.getServerName());
            statement.setBoolean(23, entry.isNotifyOnJoin());
            statement.setBoolean(24, entry.isNotificationDelivered());

            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create punishment entry.", ex);
        }
    }

    public Optional<PunishmentEntry> findActivePunishment(String minecraftName, String minecraftUuid, PunishmentType type) {
        String sql = """
                SELECT *
                FROM punishments
                WHERE punishment_type = ?
                  AND status = 'ACTIVE'
                  AND (
                        (minecraft_uuid IS NOT NULL AND minecraft_uuid = ?)
                        OR LOWER(minecraft_name) = LOWER(?)
                  )
                ORDER BY issued_at DESC
                LIMIT 1
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, type.name());
            statement.setString(2, minecraftUuid);
            statement.setString(3, minecraftName);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
            return Optional.empty();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to find active punishment.", ex);
        }
    }

    public List<PunishmentEntry> findActivePunishmentsByType(PunishmentType type) {
        List<PunishmentEntry> entries = new ArrayList<>();

        String sql = """
                SELECT *
                FROM punishments
                WHERE punishment_type = ?
                  AND status = 'ACTIVE'
                ORDER BY issued_at ASC
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, type.name());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to find active punishments by type.", ex);
        }
    }

    public List<PunishmentEntry> findAllActiveBans() {
        return findActivePunishmentsByType(PunishmentType.BAN);
    }

    public List<PunishmentEntry> findAllActiveMutes() {
        return findActivePunishmentsByType(PunishmentType.MUTE);
    }

    public List<PunishmentEntry> findActiveTimedPunishments() {
        List<PunishmentEntry> entries = new ArrayList<>();

        String sql = """
                SELECT *
                FROM punishments
                WHERE status = 'ACTIVE'
                  AND expires_at IS NOT NULL
                ORDER BY expires_at ASC
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                entries.add(map(rs));
            }
            return entries;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load active timed punishments.", ex);
        }
    }

    public List<PunishmentEntry> findExpiredActivePunishments(long now) {
        List<PunishmentEntry> entries = new ArrayList<>();

        String sql = """
                SELECT *
                FROM punishments
                WHERE status = 'ACTIVE'
                  AND expires_at IS NOT NULL
                  AND expires_at <= ?
                ORDER BY expires_at ASC
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setDateTime(statement, 1, now);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load expired active punishments.", ex);
        }
    }

    public void markRemoved(long id, long removedAt, String removedByName, String removedByUuid,
                            String removedByDiscordName, String removedByDiscordId, String removeReason) {
        String sql = """
                UPDATE punishments
                SET status = 'REMOVED',
                    removed_at = ?,
                    removed_by_name = ?,
                    removed_by_uuid = ?,
                    removed_by_discord_name = ?,
                    removed_by_discord_id = ?,
                    remove_reason = ?
                WHERE id = ?
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setDateTime(statement, 1, removedAt);
            statement.setString(2, removedByName);
            statement.setString(3, removedByUuid);
            statement.setString(4, removedByDiscordName);
            statement.setString(5, removedByDiscordId);
            statement.setString(6, removeReason);
            statement.setLong(7, id);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to mark punishment as removed.", ex);
        }
    }

    public void markHistoryRemoved(long id, long removedAt, String removedByName, String removedByUuid, String removeReason) {
        String sql = """
                UPDATE punishments
                SET removed_at = ?,
                    removed_by_name = ?,
                    removed_by_uuid = ?,
                    remove_reason = ?
                WHERE id = ?
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setDateTime(statement, 1, removedAt);
            statement.setString(2, removedByName);
            statement.setString(3, removedByUuid);
            statement.setString(4, removeReason);
            statement.setLong(5, id);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to mark punishment history as removed.", ex);
        }
    }

    public boolean markExpired(long id, long removedAt, String removeReason) {
        String sql = """
                UPDATE punishments
                SET status = 'EXPIRED',
                    removed_at = ?,
                    remove_reason = ?
                WHERE id = ?
                  AND status = 'ACTIVE'
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setDateTime(statement, 1, removedAt);
            statement.setString(2, removeReason);
            statement.setLong(3, id);
            return statement.executeUpdate() > 0;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to mark punishment as expired.", ex);
        }
    }

    public int countActiveBans() {
        String sql = """
                SELECT COUNT(*)
                FROM punishments
                WHERE punishment_type = 'BAN'
                  AND status = 'ACTIVE'
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to count active bans.", ex);
        }
    }

    public List<PunishmentEntry> findActiveBansPaged(int limit, int offset) {
        List<PunishmentEntry> entries = new ArrayList<>();

        String sql = """
                SELECT *
                FROM punishments
                WHERE punishment_type = 'BAN'
                  AND status = 'ACTIVE'
                ORDER BY issued_at DESC
                LIMIT ? OFFSET ?
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);
            statement.setInt(2, offset);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load active bans page.", ex);
        }
    }

    public int countVisiblePunishmentsByMinecraftName(String minecraftName) {
        String sql = """
                SELECT COUNT(*)
                FROM punishments
                WHERE LOWER(minecraft_name) = LOWER(?)
                  AND (remove_reason IS NULL OR remove_reason NOT LIKE ?)
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, minecraftName);
            statement.setString(2, HISTORY_CLEAR_PREFIX);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to count visible punishments by minecraft name.", ex);
        }
    }

    public List<PunishmentEntry> findVisiblePunishmentsByMinecraftNamePaged(String minecraftName, int limit, int offset) {
        List<PunishmentEntry> entries = new ArrayList<>();

        String sql = """
                SELECT *
                FROM punishments
                WHERE LOWER(minecraft_name) = LOWER(?)
                  AND (remove_reason IS NULL OR remove_reason NOT LIKE ?)
                ORDER BY issued_at DESC
                LIMIT ? OFFSET ?
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, minecraftName);
            statement.setString(2, HISTORY_CLEAR_PREFIX);
            statement.setInt(3, limit);
            statement.setInt(4, offset);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load visible paged punishments by minecraft name.", ex);
        }
    }

    public List<PunishmentEntry> findAllVisiblePunishmentsByMinecraftName(String minecraftName) {
        List<PunishmentEntry> entries = new ArrayList<>();

        String sql = """
                SELECT *
                FROM punishments
                WHERE LOWER(minecraft_name) = LOWER(?)
                  AND (remove_reason IS NULL OR remove_reason NOT LIKE ?)
                ORDER BY issued_at DESC
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, minecraftName);
            statement.setString(2, HISTORY_CLEAR_PREFIX);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load all visible punishments by minecraft name.", ex);
        }
    }

    public List<Long> findVisiblePunishmentIdsByMinecraftName(String minecraftName) {
        List<Long> ids = new ArrayList<>();

        String sql = """
                SELECT id
                FROM punishments
                WHERE LOWER(minecraft_name) = LOWER(?)
                  AND (remove_reason IS NULL OR remove_reason NOT LIKE ?)
                  AND status <> 'ACTIVE'
                ORDER BY issued_at DESC
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, minecraftName);
            statement.setString(2, HISTORY_CLEAR_PREFIX);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                }
            }
            return ids;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load visible punishment ids by minecraft name.", ex);
        }
    }

    public List<PunishmentEntry> findRecentPunishmentsByMinecraftName(String minecraftName, int limit) {
        List<PunishmentEntry> entries = new ArrayList<>();

        String sql = """
                SELECT *
                FROM punishments
                WHERE LOWER(minecraft_name) = LOWER(?)
                  AND (remove_reason IS NULL OR remove_reason NOT LIKE ?)
                ORDER BY issued_at DESC
                LIMIT ?
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, minecraftName);
            statement.setString(2, HISTORY_CLEAR_PREFIX);
            statement.setInt(3, limit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load punishment history by minecraft name.", ex);
        }
    }

    public List<PunishmentEntry> findRecentPunishmentsByDiscordId(String discordId, int limit) {
        List<PunishmentEntry> entries = new ArrayList<>();

        String sql = """
                SELECT *
                FROM punishments
                WHERE discord_id = ?
                  AND (remove_reason IS NULL OR remove_reason NOT LIKE ?)
                ORDER BY issued_at DESC
                LIMIT ?
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, discordId);
            statement.setString(2, HISTORY_CLEAR_PREFIX);
            statement.setInt(3, limit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load punishment history by discord id.", ex);
        }
    }

    public Optional<PunishmentEntry> findPunishmentById(long id) {
        String sql = """
                SELECT *
                FROM punishments
                WHERE id = ?
                LIMIT 1
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }

            return Optional.empty();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to find punishment by id.", ex);
        }
    }

    public List<PunishmentEntry> getPendingNotifications(UUID uuid) {
        List<PunishmentEntry> entries = new ArrayList<>();

        String sql = """
                SELECT *
                FROM punishments
                WHERE minecraft_uuid = ?
                  AND notify_on_join = ?
                  AND notification_delivered = ?
                ORDER BY issued_at DESC
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, uuid.toString());
            statement.setBoolean(2, true);
            statement.setBoolean(3, false);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load pending punishment notifications.", ex);
        }
    }

    public void markNotificationDelivered(long id) {
        String sql = """
                UPDATE punishments
                SET notification_delivered = ?
                WHERE id = ?
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, true);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to mark punishment notification as delivered.", ex);
        }
    }

    private PunishmentEntry map(ResultSet rs) throws Exception {
        PunishmentEntry entry = new PunishmentEntry();

        entry.setId(rs.getLong("id"));
        entry.setDiscordName(rs.getString("discord_name"));
        entry.setDiscordId(rs.getString("discord_id"));
        entry.setMinecraftName(rs.getString("minecraft_name"));
        entry.setMinecraftUuid(rs.getString("minecraft_uuid"));
        entry.setPunishmentType(PunishmentType.fromString(rs.getString("punishment_type")));
        entry.setReason(rs.getString("reason"));
        entry.setIssuedByName(rs.getString("issued_by_name"));
        entry.setIssuedByUuid(rs.getString("issued_by_uuid"));
        entry.setIssuedByDiscordName(rs.getString("issued_by_discord_name"));
        entry.setIssuedByDiscordId(rs.getString("issued_by_discord_id"));
        entry.setIssuedSource(PunishmentSource.fromString(rs.getString("issued_source")));
        entry.setDurationMs(getNullableLong(rs, "duration_ms"));
        entry.setIssuedAt(getDateTimeMillis(rs, "issued_at"));
        entry.setExpiresAt(getNullableDateTimeMillis(rs, "expires_at"));
        entry.setStatus(PunishmentStatus.fromString(rs.getString("status")));
        entry.setRemovedAt(getNullableDateTimeMillis(rs, "removed_at"));
        entry.setRemovedByName(rs.getString("removed_by_name"));
        entry.setRemovedByUuid(rs.getString("removed_by_uuid"));
        entry.setRemovedByDiscordName(rs.getString("removed_by_discord_name"));
        entry.setRemovedByDiscordId(rs.getString("removed_by_discord_id"));
        entry.setRemoveReason(rs.getString("remove_reason"));
        entry.setServerName(rs.getString("server_name"));
        entry.setNotifyOnJoin(getBoolean(rs, "notify_on_join"));
        entry.setNotificationDelivered(getBoolean(rs, "notification_delivered"));

        return entry;
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws Exception {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private Long getNullableLong(ResultSet rs, String column) throws Exception {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private boolean getBoolean(ResultSet rs, String column) throws Exception {
        boolean value = rs.getBoolean(column);
        return !rs.wasNull() && value;
    }

    private void setDateTime(PreparedStatement statement, int index, long epochMillis) throws Exception {
        if (isMySql()) {
            statement.setTimestamp(index, new Timestamp(epochMillis));
        } else {
            statement.setString(index, formatSqliteDateTime(epochMillis));
        }
    }

    private void setNullableDateTime(PreparedStatement statement, int index, Long epochMillis) throws Exception {
        if (epochMillis == null) {
            if (isMySql()) {
                statement.setNull(index, Types.TIMESTAMP);
            } else {
                statement.setNull(index, Types.VARCHAR);
            }
            return;
        }

        setDateTime(statement, index, epochMillis);
    }

    private long getDateTimeMillis(ResultSet rs, String column) throws Exception {
        Long value = getNullableDateTimeMillis(rs, column);
        return value == null ? 0L : value;
    }

    private Long getNullableDateTimeMillis(ResultSet rs, String column) throws Exception {
        if (isMySql()) {
            Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : timestamp.getTime();
        }

        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }

        return parseSqliteDateTime(value);
    }

    private boolean isMySql() {
        return plugin.getDatabaseManager().getDatabaseType() != null
                && plugin.getDatabaseManager().getDatabaseType().name().equalsIgnoreCase("MYSQL");
    }

    private String formatSqliteDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC)
                .format(SQLITE_DATE_TIME_FORMAT);
    }

    private long parseSqliteDateTime(String value) {
        LocalDateTime dateTime = LocalDateTime.parse(value, SQLITE_DATE_TIME_FORMAT);
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}