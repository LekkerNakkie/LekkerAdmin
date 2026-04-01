package me.lekkernakkie.lekkeradmin.database.repository;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.DatabaseManager;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class LinkRepository {

    private final DatabaseManager databaseManager;

    public LinkRepository(LekkerAdmin plugin) {
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * Optie B gebruikt geen aparte link-tabel meer.
     * Een "link" zit in whitelist_entries zodra minecraft_uuid/link info aanwezig is.
     */
    public void save(DiscordMinecraftLink link) {
        String sql = """
                UPDATE whitelist_entries
                SET discord_tag = ?,
                    minecraft_uuid = ?,
                    minecraft_name = ?,
                    linked_at = ?
                WHERE application_id = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, link.getDiscordTag());
            statement.setString(2, link.getMinecraftUuid());
            statement.setString(3, link.getMinecraftName());

            if (link.getLinkedAt() == null) {
                statement.setLong(4, System.currentTimeMillis());
            } else {
                statement.setLong(4, link.getLinkedAt());
            }

            statement.setString(5, link.getLinkedByApplicationId());
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save link for application: " + link.getLinkedByApplicationId(), ex);
        }
    }

    public Optional<DiscordMinecraftLink> findByDiscordUserId(String discordUserId) {
        String sql = """
                SELECT id,
                       discord_user_id,
                       discord_tag,
                       minecraft_uuid,
                       minecraft_name,
                       application_id,
                       linked_at
                FROM whitelist_entries
                WHERE discord_user_id = ?
                  AND minecraft_uuid IS NOT NULL
                  AND TRIM(minecraft_uuid) <> ''
                LIMIT 1
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, discordUserId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load link by discord user id: " + discordUserId, ex);
        }
    }

    public Optional<DiscordMinecraftLink> findByMinecraftName(String minecraftName) {
        String sql = """
                SELECT id,
                       discord_user_id,
                       discord_tag,
                       minecraft_uuid,
                       minecraft_name,
                       application_id,
                       linked_at
                FROM whitelist_entries
                WHERE minecraft_name = ?
                  AND minecraft_uuid IS NOT NULL
                  AND TRIM(minecraft_uuid) <> ''
                LIMIT 1
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, minecraftName);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load link by minecraft name: " + minecraftName, ex);
        }
    }

    private DiscordMinecraftLink map(ResultSet rs) throws Exception {
        DiscordMinecraftLink link = new DiscordMinecraftLink();
        link.setId(rs.getLong("id"));
        link.setDiscordUserId(rs.getString("discord_user_id"));
        link.setDiscordTag(rs.getString("discord_tag"));
        link.setMinecraftUuid(rs.getString("minecraft_uuid"));
        link.setMinecraftName(rs.getString("minecraft_name"));
        link.setLinkedByApplicationId(rs.getString("application_id"));

        long linkedAt = rs.getLong("linked_at");
        link.setLinkedAt(rs.wasNull() ? null : linkedAt);

        return link;
    }
}