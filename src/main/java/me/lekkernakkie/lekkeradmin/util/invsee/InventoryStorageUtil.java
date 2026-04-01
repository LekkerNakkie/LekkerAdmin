package me.lekkernakkie.lekkeradmin.util.invsee;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class InventoryStorageUtil {

    private InventoryStorageUtil() {
    }

    public static String serializeInventoryData(ItemStack[] storageContents, ItemStack[] armorContents, ItemStack offhand) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(byteOut)) {
                out.writeObject(storageContents);
                out.writeObject(armorContents);
                out.writeObject(offhand);
            }
            return Base64.getEncoder().encodeToString(byteOut.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize inventory data", ex);
        }
    }

    public static InventorySnapshot deserializeInventoryData(String data) {
        if (data == null || data.isBlank()) {
            return new InventorySnapshot(new ItemStack[36], new ItemStack[4], null);
        }

        try {
            byte[] raw = Base64.getDecoder().decode(data);
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(raw))) {
                ItemStack[] storage = (ItemStack[]) in.readObject();
                ItemStack[] armor = (ItemStack[]) in.readObject();
                ItemStack offhand = (ItemStack) in.readObject();

                return new InventorySnapshot(
                        storage == null ? new ItemStack[36] : storage,
                        armor == null ? new ItemStack[4] : armor,
                        offhand
                );
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to deserialize inventory data", ex);
        }
    }

    public static String serializeItemArray(ItemStack[] items) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(byteOut)) {
                out.writeObject(items);
            }
            return Base64.getEncoder().encodeToString(byteOut.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize item array", ex);
        }
    }

    public static ItemStack[] deserializeItemArray(String data, int expectedLength) {
        if (data == null || data.isBlank()) {
            return new ItemStack[expectedLength];
        }

        try {
            byte[] raw = Base64.getDecoder().decode(data);
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(raw))) {
                ItemStack[] items = (ItemStack[]) in.readObject();
                return items == null ? new ItemStack[expectedLength] : items;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to deserialize item array", ex);
        }
    }

    public record InventorySnapshot(
            ItemStack[] storageContents,
            ItemStack[] armorContents,
            ItemStack offhand
    ) {
    }
}