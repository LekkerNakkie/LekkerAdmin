package me.lekkernakkie.lekkeradmin.service.invsee;

import me.lekkernakkie.lekkeradmin.model.invsee.InvseeTargetData;
import me.lekkernakkie.lekkeradmin.util.invsee.InventorySerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

public class OnlineInventoryService {

    public InvseeTargetData load(Player target) {
        PlayerInventory inv = target.getInventory();

        return new InvseeTargetData(
                target.getUniqueId(),
                target.getName(),
                true,
                InventorySerializer.cloneArray(inv.getStorageContents()),
                InventorySerializer.cloneArray(inv.getArmorContents()),
                InventorySerializer.cloneItem(inv.getItemInOffHand())
        );
    }

    public void save(Player target, org.bukkit.inventory.Inventory gui) {
        PlayerInventory inv = target.getInventory();

        inv.setStorageContents(extractStorage(gui));
        inv.setArmorContents(extractArmor(gui));
        inv.setItemInOffHand(gui.getItem(49));
    }

    private org.bukkit.inventory.ItemStack[] extractStorage(org.bukkit.inventory.Inventory gui) {
        org.bukkit.inventory.ItemStack[] contents = new org.bukkit.inventory.ItemStack[36];
        for (int i = 0; i < 36; i++) {
            contents[i] = gui.getItem(i) == null ? null : gui.getItem(i).clone();
        }
        return contents;
    }

    private org.bukkit.inventory.ItemStack[] extractArmor(org.bukkit.inventory.Inventory gui) {
        org.bukkit.inventory.ItemStack[] armor = new org.bukkit.inventory.ItemStack[4];
        armor[0] = gui.getItem(45) == null ? null : gui.getItem(45).clone();
        armor[1] = gui.getItem(44) == null ? null : gui.getItem(44).clone();
        armor[2] = gui.getItem(43) == null ? null : gui.getItem(43).clone();
        armor[3] = gui.getItem(42) == null ? null : gui.getItem(42).clone();
        return armor;
    }
}