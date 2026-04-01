package me.lekkernakkie.lekkeradmin.model.invsee;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class InvseeTargetData {

    private final UUID targetUuid;
    private final String targetName;
    private final boolean online;
    private final ItemStack[] contents;
    private final ItemStack[] armorContents;
    private final ItemStack offhand;

    public InvseeTargetData(
            UUID targetUuid,
            String targetName,
            boolean online,
            ItemStack[] contents,
            ItemStack[] armorContents,
            ItemStack offhand
    ) {
        this.targetUuid = targetUuid;
        this.targetName = targetName == null ? "-" : targetName;
        this.online = online;
        this.contents = contents == null ? new ItemStack[36] : contents;
        this.armorContents = armorContents == null ? new ItemStack[4] : armorContents;
        this.offhand = offhand;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public boolean isOnline() {
        return online;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public ItemStack[] getArmorContents() {
        return armorContents;
    }

    public ItemStack getOffhand() {
        return offhand;
    }
}