package me.lekkernakkie.lekkeradmin.database.repository;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.invsee.InvseeTargetData;
import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;
import me.lekkernakkie.lekkeradmin.service.link.LinkLookupService;
import me.lekkernakkie.lekkeradmin.util.invsee.InventorySerializer;
import me.lekkernakkie.lekkeradmin.util.invsee.InventoryStorageUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

public class PendingChangesRepository {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LekkerAdmin plugin;
    private final LinkLookupService linkLookupService;

    public PendingChangesRepository(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.linkLookupService = new LinkLookupService(plugin);
    }

    public InvseeTargetData loadInventoryData(OfflinePlayer target) {
        PendingRow row = findByUuid(target.getUniqueId());

        if (row == null || row.inventoryData() == null || row.inventoryData().isBlank()) {
            return new InvseeTargetData(
                    target.getUniqueId(),
                    target.getName() == null ? "-" : target.getName(),
                    false,
                    new ItemStack[36],
                    new ItemStack[4],
                    null
            );
        }

        InventoryStorageUtil.InventorySnapshot snapshot =
                InventoryStorageUtil.deserializeInventoryData(row.inventoryData());

        return new InvseeTargetData(
                target.getUniqueId(),
                target.getName() == null ? "-" : target.getName(),
                false,
                InventorySerializer.cloneArray(snapshot.storageContents()),
                InventorySerializer.cloneArray(snapshot.armorContents()),
                InventorySerializer.cloneItem(snapshot.offhand())
        );
    }

    public ItemStack[] loadEnderChestData(OfflinePlayer target) {
        PendingRow row = findByUuid(target.getUniqueId());
        if (row == null || row.enderchestData() == null || row.enderchestData().isBlank()) {
            return new ItemStack[27];
        }

        return InventorySerializer.cloneArray(
                InventoryStorageUtil.deserializeItemArray(row.enderchestData(), 27)
        );
    }

    public void saveOfflineInventoryData(OfflinePlayer target,
                                         ItemStack[] storage,
                                         ItemStack[] armor,
                                         ItemStack offhand,
                                         String editedBy) {

        PendingRow existing = findByUuid(target.getUniqueId());
        LinkData linkData = resolveLinkData(target);

        String inventoryData = InventoryStorageUtil.serializeInventoryData(storage, armor, offhand);
        String enderchestData = existing == null ? "" : nullSafe(existing.enderchestData());

        upsertRow(
                target.getUniqueId(),
                target.getName(),
                linkData.discordName(),
                linkData.discordId(),
                inventoryData,
                enderchestData,
                existing == null ? null : existing.lastOnline(),
                now(),
                existing == null ? null : existing.lastApplied(),
                editedBy,
                true
        );
    }

    public void saveOfflineEnderChestData(OfflinePlayer target,
                                          ItemStack[] enderChest,
                                          String editedBy) {

        PendingRow existing = findByUuid(target.getUniqueId());
        LinkData linkData = resolveLinkData(target);

        String inventoryData = existing == null ? "" : nullSafe(existing.inventoryData());
        String enderchestData = InventoryStorageUtil.serializeItemArray(enderChest);

        upsertRow(
                target.getUniqueId(),
                target.getName(),
                linkData.discordName(),
                linkData.discordId(),
                inventoryData,
                enderchestData,
                existing == null ? null : existing.lastOnline(),
                now(),
                existing == null ? null : existing.lastApplied(),
                editedBy,
                true
        );
    }

    public void saveOnlineSnapshot(Player player) {
        PlayerInventory inv = player.getInventory();
        LinkData linkData = resolveLinkData(player);

        String inventoryData = InventoryStorageUtil.serializeInventoryData(
                inv.getStorageContents(),
                inv.getArmorContents(),
                inv.getItemInOffHand()
        );

        String enderchestData = InventoryStorageUtil.serializeItemArray(player.getEnderChest().getContents());

        PendingRow existing = findByUuid(player.getUniqueId());

        upsertRow(
                player.getUniqueId(),
                player.getName(),
                linkData.discordName(),
                linkData.discordId(),
                inventoryData,
                enderchestData,
                now(),
                existing == null ? null : existing.lastEdited(),
                existing == null ? null : existing.lastApplied(),
                existing == null ? null : existing.editedBy(),
                existing != null && existing.hasPendingChanges()
        );
    }

    public boolean applyPendingChanges(Player player) {
        PendingRow row = findByUuid(player.getUniqueId());
        if (row == null || !row.hasPendingChanges()) {
            return false;
        }

        if (row.inventoryData() != null && !row.inventoryData().isBlank()) {
            InventoryStorageUtil.InventorySnapshot snapshot =
                    InventoryStorageUtil.deserializeInventoryData(row.inventoryData());

            PlayerInventory inv = player.getInventory();
            inv.setStorageContents(InventorySerializer.cloneArray(snapshot.storageContents()));
            inv.setArmorContents(InventorySerializer.cloneArray(snapshot.armorContents()));
            inv.setItemInOffHand(InventorySerializer.cloneItem(snapshot.offhand()));
        }

        if (row.enderchestData() != null && !row.enderchestData().isBlank()) {
            ItemStack[] enderChest = InventoryStorageUtil.deserializeItemArray(row.enderchestData(), 27);
            player.getEnderChest().setContents(InventorySerializer.cloneArray(enderChest));
        }

        LinkData linkData = resolveLinkData(player);

        upsertRow(
                player.getUniqueId(),
                player.getName(),
                linkData.discordName(),
                linkData.discordId(),
                row.inventoryData(),
                row.enderchestData(),
                now(),
                row.lastEdited(),
                now(),
                row.editedBy(),
                false
        );

        player.updateInventory();
        return true;
    }

    private PendingRow findByUuid(UUID uuid) {
        String sql = """
                SELECT minecraft_name, minecraft_uuid, discord_name, discord_id,
                       inventory_data, enderchest_data, last_online, last_edited,
                       last_applied, edited_by, has_pending_changes
                FROM pending_changes
                WHERE minecraft_uuid = ?
                """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, uuid.toString());

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new PendingRow(
                        rs.getString("minecraft_name"),
                        rs.getString("minecraft_uuid"),
                        rs.getString("discord_name"),
                        rs.getString("discord_id"),
                        rs.getString("inventory_data"),
                        rs.getString("enderchest_data"),
                        rs.getString("last_online"),
                        rs.getString("last_edited"),
                        rs.getString("last_applied"),
                        rs.getString("edited_by"),
                        readBoolean(rs, "has_pending_changes")
                );
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load pending_changes row for " + uuid, ex);
        }
    }

    private void upsertRow(UUID uuid,
                           String minecraftName,
                           String discordName,
                           String discordId,
                           String inventoryData,
                           String enderchestData,
                           String lastOnline,
                           String lastEdited,
                           String lastApplied,
                           String editedBy,
                           boolean hasPendingChanges) {

        boolean mysql = plugin.getDatabaseManager().getDatabaseType() != null
                && plugin.getDatabaseManager().getDatabaseType().name().equalsIgnoreCase("MYSQL");

        String sql = mysql
                ? """
                  INSERT INTO pending_changes (
                      minecraft_name, minecraft_uuid, discord_name, discord_id,
                      inventory_data, enderchest_data, last_online, last_edited,
                      last_applied, edited_by, has_pending_changes
                  ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                  ON DUPLICATE KEY UPDATE
                      minecraft_name = VALUES(minecraft_name),
                      discord_name = VALUES(discord_name),
                      discord_id = VALUES(discord_id),
                      inventory_data = VALUES(inventory_data),
                      enderchest_data = VALUES(enderchest_data),
                      last_online = VALUES(last_online),
                      last_edited = VALUES(last_edited),
                      last_applied = VALUES(last_applied),
                      edited_by = VALUES(edited_by),
                      has_pending_changes = VALUES(has_pending_changes)
                  """
                : """
                  INSERT INTO pending_changes (
                      minecraft_name, minecraft_uuid, discord_name, discord_id,
                      inventory_data, enderchest_data, last_online, last_edited,
                      last_applied, edited_by, has_pending_changes
                  ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                  ON CONFLICT(minecraft_uuid) DO UPDATE SET
                      minecraft_name = excluded.minecraft_name,
                      discord_name = excluded.discord_name,
                      discord_id = excluded.discord_id,
                      inventory_data = excluded.inventory_data,
                      enderchest_data = excluded.enderchest_data,
                      last_online = excluded.last_online,
                      last_edited = excluded.last_edited,
                      last_applied = excluded.last_applied,
                      edited_by = excluded.edited_by,
                      has_pending_changes = excluded.has_pending_changes
                  """;

        try (Connection connection = plugin.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, nullSafe(minecraftName));
            statement.setString(2, uuid.toString());
            statement.setString(3, nullSafe(discordName));
            statement.setString(4, nullSafe(discordId));
            statement.setString(5, nullSafe(inventoryData));
            statement.setString(6, nullSafe(enderchestData));
            statement.setString(7, lastOnline);
            statement.setString(8, lastEdited);
            statement.setString(9, lastApplied);
            statement.setString(10, editedBy);
            statement.setBoolean(11, hasPendingChanges);

            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upsert pending_changes row for " + uuid, ex);
        }
    }

    private LinkData resolveLinkData(OfflinePlayer target) {
        if (target.getName() == null || target.getName().isBlank()) {
            return new LinkData("", "");
        }

        Optional<DiscordMinecraftLink> optional = linkLookupService.findByMinecraftName(target.getName());
        if (optional.isEmpty()) {
            return new LinkData("", "");
        }

        DiscordMinecraftLink link = optional.get();
        return new LinkData(
                nullSafe(link.getDiscordTag()),
                nullSafe(link.getDiscordUserId())
        );
    }

    private boolean readBoolean(ResultSet rs, String column) throws Exception {
        Object value = rs.getObject(column);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        if (value instanceof String s) {
            return s.equalsIgnoreCase("true") || s.equals("1");
        }
        return false;
    }

    private String now() {
        return ZonedDateTime.now(ZONE).format(FORMATTER);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private record LinkData(String discordName, String discordId) {
    }

    private record PendingRow(
            String minecraftName,
            String minecraftUuid,
            String discordName,
            String discordId,
            String inventoryData,
            String enderchestData,
            String lastOnline,
            String lastEdited,
            String lastApplied,
            String editedBy,
            boolean hasPendingChanges
    ) {
    }
}