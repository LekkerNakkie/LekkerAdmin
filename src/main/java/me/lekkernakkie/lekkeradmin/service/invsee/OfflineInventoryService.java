package me.lekkernakkie.lekkeradmin.service.invsee;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.MainConfig;
import me.lekkernakkie.lekkeradmin.model.invsee.InvseeTargetData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class OfflineInventoryService {

    private final LekkerAdmin plugin;
    private final OfflinePlayerDataStore dataStore;

    public OfflineInventoryService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.dataStore = new OfflinePlayerDataStore(OfflinePlayerDataStore.resolveDefaultWorldFolder());
    }

    public InvseeTargetData load(OfflinePlayer target) {
        OfflinePlayerDataStore.InventorySnapshot snapshot = dataStore.loadInventory(target);

        String targetName = target.getName() == null ? "-" : target.getName();

        return new InvseeTargetData(
                target.getUniqueId(),
                targetName,
                false,
                cloneArray(snapshot.getContents(), 36),
                cloneArray(snapshot.getArmor(), 4),
                clone(snapshot.getOffhand())
        );
    }

    public void save(OfflinePlayer target, Inventory gui, String editorName) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            contents[i] = clone(gui.getItem(i));
        }

        ItemStack[] armor = new ItemStack[4];
        armor[3] = clone(gui.getItem(config.getInvseeHelmetSlot()));
        armor[2] = clone(gui.getItem(config.getInvseeChestplateSlot()));
        armor[1] = clone(gui.getItem(config.getInvseeLeggingsSlot()));
        armor[0] = clone(gui.getItem(config.getInvseeBootsSlot()));

        ItemStack offhand = clone(gui.getItem(config.getInvseeOffhandSlot()));

        dataStore.saveInventory(target, contents, armor, offhand);
    }

    public Inventory loadEnderChest(OfflinePlayer target) {
        String safeName = target.getName() == null ? "-" : target.getName();
        String title = org.bukkit.ChatColor.translateAlternateColorCodes(
                '&',
                plugin.getConfigManager().getMainConfig().getEnderchestTitle().replace("{player}", safeName)
        );

        Inventory inventory = Bukkit.createInventory(null, 27, title);
        ItemStack[] contents = dataStore.loadEnderChest(target);

        for (int i = 0; i < Math.min(contents.length, 27); i++) {
            inventory.setItem(i, clone(contents[i]));
        }

        return inventory;
    }

    public void saveEnderChest(OfflinePlayer target, Inventory gui, String editorName) {
        ItemStack[] contents = new ItemStack[27];
        for (int i = 0; i < 27; i++) {
            contents[i] = clone(gui.getItem(i));
        }

        dataStore.saveEnderChest(target, contents);
    }

    public void saveOnlineSnapshot(org.bukkit.entity.Player player) {
        // Niet meer nodig voor echte offline invsee.
    }

    public boolean applyPendingChanges(org.bukkit.entity.Player player) {
        // Niet meer nodig voor echte offline invsee.
        return false;
    }

    private ItemStack clone(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private ItemStack[] cloneArray(ItemStack[] input, int expectedSize) {
        ItemStack[] out = new ItemStack[expectedSize];
        if (input == null) {
            return out;
        }

        for (int i = 0; i < Math.min(input.length, expectedSize); i++) {
            out[i] = clone(input[i]);
        }
        return out;
    }
}