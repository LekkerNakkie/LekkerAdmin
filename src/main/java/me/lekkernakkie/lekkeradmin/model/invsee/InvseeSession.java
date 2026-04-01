package me.lekkernakkie.lekkeradmin.model.invsee;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvseeSession {

    private final UUID viewerUuid;
    private final String viewerName;
    private final OfflinePlayer target;
    private final boolean onlineTarget;
    private final boolean readOnly;
    private final Inventory inventory;
    private final boolean enderChest;
    private final long openedAt;
    private long closedAt;

    private final ItemStack[] startContents;
    private final List<String> actions = new ArrayList<>();
    private boolean manualActionRecorded = false;

    public InvseeSession(Player viewer,
                         OfflinePlayer target,
                         boolean onlineTarget,
                         boolean readOnly,
                         Inventory inventory,
                         boolean enderChest) {
        this.viewerUuid = viewer.getUniqueId();
        this.viewerName = viewer.getName();
        this.target = target;
        this.onlineTarget = onlineTarget;
        this.readOnly = readOnly;
        this.inventory = inventory;
        this.enderChest = enderChest;
        this.openedAt = System.currentTimeMillis();
        this.startContents = cloneContents(inventory.getContents());
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clone[i] = contents[i] == null ? null : contents[i].clone();
        }
        return clone;
    }

    public void finalizeSession() {
        this.closedAt = System.currentTimeMillis();

        if (onlineTarget) {
            finalizeOnlineSession();
        } else {
            compareOfflineChanges();
        }
    }

    private void finalizeOnlineSession() {
        if (actions.isEmpty()) {
            actions.add("Geen wijzigingen gemaakt.");
        }
    }

    private void compareOfflineChanges() {
        ItemStack[] end = inventory.getContents();

        Map<String, Integer> startMap = toMap(startContents);
        Map<String, Integer> endMap = toMap(end);

        for (String key : startMap.keySet()) {
            int startAmount = startMap.getOrDefault(key, 0);
            int endAmount = endMap.getOrDefault(key, 0);

            if (startAmount > endAmount) {
                int diff = startAmount - endAmount;
                actions.add("Uitgenomen: " + key + " x" + diff);
            }
        }

        for (String key : endMap.keySet()) {
            int startAmount = startMap.getOrDefault(key, 0);
            int endAmount = endMap.getOrDefault(key, 0);

            if (endAmount > startAmount) {
                int diff = endAmount - startAmount;
                actions.add("Geplaatst: " + key + " x" + diff);
            }
        }

        if (actions.isEmpty()) {
            actions.add("Geen wijzigingen gemaakt.");
        }
    }

    private Map<String, Integer> toMap(ItemStack[] contents) {
        Map<String, Integer> map = new HashMap<>();

        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            String name = item.getType().name();
            map.put(name, map.getOrDefault(name, 0) + item.getAmount());
        }

        return map;
    }

    public void recordManualAction(String action) {
        if (action == null || action.isBlank()) {
            return;
        }

        actions.add(action);
        manualActionRecorded = true;
    }

    public boolean hasManualActionRecorded() {
        return manualActionRecorded;
    }

    public boolean hasMeaningfulChanges() {
        return actions.stream().anyMatch(action -> action != null && !action.equalsIgnoreCase("Geen wijzigingen gemaakt."));
    }

    public List<String> getActions() {
        return actions;
    }

    public long getOpenedAt() {
        return openedAt;
    }

    public long getClosedAt() {
        return closedAt;
    }

    public long getDurationMillis() {
        return closedAt - openedAt;
    }

    public String getViewerName() {
        return viewerName;
    }

    public OfflinePlayer getTarget() {
        return target;
    }

    public boolean isOnlineTarget() {
        return onlineTarget;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public boolean isEnderChest() {
        return enderChest;
    }
}