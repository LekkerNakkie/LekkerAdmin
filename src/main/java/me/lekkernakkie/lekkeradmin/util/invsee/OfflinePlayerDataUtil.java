package me.lekkernakkie.lekkeradmin.util.invsee;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

public class OfflinePlayerDataUtil {

    // 🔹 LOAD inventory (including armor + offhand)
    public static Inventory load(OfflinePlayer player) {
        Inventory inv = Bukkit.createInventory(null, 54, "Offline Inventory");

        // ⚠️ BELANGRIJK:
        // Dit is fallback — Bukkit kan geen echte offline inventory lezen
        // Dus we tonen gewoon lege inventory voorlopig

        return inv;
    }

    // 🔹 SAVE inventory (future-proof)
    public static void save(OfflinePlayer player, Inventory inventory) {
        // TODO later uitbreiden met echte NBT save
    }

    // 🔹 DEBUG (BELANGRIJK VOOR JOU NU)
    public static void debugInventoryData(OfflinePlayer player, Logger logger) {
        logger.info("===== INVSEE DEBUG =====");
        logger.info("Player: " + player.getName());
        logger.info("UUID: " + player.getUniqueId());

        if (player.isOnline()) {
            logger.info("Player is ONLINE → realtime inventory wordt gebruikt");
        } else {
            logger.warning("Player is OFFLINE → Bukkit kan GEEN armor/offhand lezen!");
            logger.warning("=> Daarom zie je armor/offhand niet");
        }

        logger.info("========================");
    }
}