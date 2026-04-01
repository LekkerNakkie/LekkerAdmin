package me.lekkernakkie.lekkeradmin.util.invsee;

import org.bukkit.inventory.ItemStack;

public final class InventorySerializer {

    private InventorySerializer() {
    }

    public static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    public static ItemStack[] cloneArray(ItemStack[] input) {
        if (input == null) {
            return new ItemStack[0];
        }

        ItemStack[] copy = new ItemStack[input.length];
        for (int i = 0; i < input.length; i++) {
            copy[i] = input[i] == null ? null : input[i].clone();
        }
        return copy;
    }
}