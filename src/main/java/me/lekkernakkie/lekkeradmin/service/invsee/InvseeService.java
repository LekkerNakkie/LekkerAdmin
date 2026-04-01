package me.lekkernakkie.lekkeradmin.service.invsee;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.MainConfig;
import me.lekkernakkie.lekkeradmin.model.invsee.InvseeSession;
import me.lekkernakkie.lekkeradmin.model.invsee.InvseeTargetData;
import me.lekkernakkie.lekkeradmin.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InvseeService {

    private final LekkerAdmin plugin;
    private final OnlineInventoryService onlineInventoryService;
    private final OfflineInventoryService offlineInventoryService;
    private final InvseeLogService logService;

    private final Map<UUID, InvseeSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> liveSyncTasks = new ConcurrentHashMap<>();

    /**
     * key   = target uuid
     * value = viewer uuid
     *
     * Hiermee voorkomen we dat meerdere staffleden tegelijk dezelfde offline
     * inventory / enderchest bewerken.
     */
    private final Map<UUID, UUID> offlineTargetEditors = new ConcurrentHashMap<>();

    public InvseeService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.onlineInventoryService = new OnlineInventoryService();
        this.offlineInventoryService = new OfflineInventoryService(plugin);
        this.logService = new InvseeLogService(plugin);
    }

    public void openOnlineInventory(Player viewer, Player target) {
        InvseeTargetData data = onlineInventoryService.load(target);
        boolean readOnly = !plugin.getConfigManager().getMainConfig().isInvseeAllowModify()
                || !viewer.hasPermission("lekkeradmin.invsee.modify");

        Inventory gui = buildInventory(data);

        InvseeSession session = new InvseeSession(viewer, target, true, readOnly, gui, false);
        sessions.put(viewer.getUniqueId(), session);

        viewer.openInventory(gui);
        startLiveSync(viewer, target, false);
    }

    public void openOfflinePlaceholder(Player viewer, OfflinePlayer target) {
        if (!tryLockOfflineTarget(viewer, target)) {
            viewer.sendMessage(plugin.lang().message(
                    "invsee.offline-inventory-in-use",
                    "&7Deze offline inventory wordt momenteel al door iemand anders &bbewerkt&7."
            ));
            return;
        }

        try {
            InvseeTargetData data = offlineInventoryService.load(target);
            boolean readOnly = !plugin.getConfigManager().getMainConfig().isInvseeAllowModify()
                    || !viewer.hasPermission("lekkeradmin.invsee.modify");

            Inventory gui = buildInventory(data);

            InvseeSession session = new InvseeSession(viewer, target, false, readOnly, gui, false);
            sessions.put(viewer.getUniqueId(), session);

            viewer.openInventory(gui);
        } catch (Exception ex) {
            offlineTargetEditors.remove(target.getUniqueId(), viewer.getUniqueId());
            viewer.sendMessage(plugin.lang().formatMessage(
                    "invsee.offline-inventory-load-failed",
                    "&cKon offline inventory niet laden: &7{error}",
                    Map.of("error", ex.getMessage() == null ? "-" : ex.getMessage())
            ));
            plugin.getLogger().warning("Failed to load offline invsee for " + target.getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void openOnlineEnderChest(Player viewer, Player target) {
        boolean readOnly = !plugin.getConfigManager().getMainConfig().isEnderchestAllowModify()
                || !viewer.hasPermission("lekkeradmin.enderchest.modify");

        Inventory gui = Bukkit.createInventory(null, 27, buildEnderChestTitle(target.getName()));

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, cloneItem(target.getEnderChest().getItem(i)));
        }

        InvseeSession session = new InvseeSession(viewer, target, true, readOnly, gui, true);
        sessions.put(viewer.getUniqueId(), session);

        viewer.openInventory(gui);
        startLiveSync(viewer, target, true);
    }

    public void openOfflineEnderChest(Player viewer, OfflinePlayer target) {
        if (!tryLockOfflineTarget(viewer, target)) {
            viewer.sendMessage(plugin.lang().message(
                    "invsee.offline-enderchest-in-use",
                    "&7Deze offline enderchest wordt momenteel al door iemand anders &bbewerkt&7."
            ));
            return;
        }

        try {
            boolean readOnly = !plugin.getConfigManager().getMainConfig().isEnderchestAllowModify()
                    || !viewer.hasPermission("lekkeradmin.enderchest.modify");

            Inventory gui = offlineInventoryService.loadEnderChest(target);

            InvseeSession session = new InvseeSession(viewer, target, false, readOnly, gui, true);
            sessions.put(viewer.getUniqueId(), session);

            viewer.openInventory(gui);
        } catch (Exception ex) {
            offlineTargetEditors.remove(target.getUniqueId(), viewer.getUniqueId());
            viewer.sendMessage(plugin.lang().formatMessage(
                    "invsee.offline-enderchest-load-failed",
                    "&cKon offline enderchest niet laden: &7{error}",
                    Map.of("error", ex.getMessage() == null ? "-" : ex.getMessage())
            ));
            plugin.getLogger().warning("Failed to load offline enderchest for " + target.getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public InvseeSession getSession(Player viewer) {
        return sessions.get(viewer.getUniqueId());
    }

    public void closeSession(Player viewer) {
        stopLiveSync(viewer.getUniqueId());

        InvseeSession session = sessions.remove(viewer.getUniqueId());
        if (session == null) {
            return;
        }

        try {
            session.finalizeSession();

            if (!session.isReadOnly() && !session.isOnlineTarget() && session.hasMeaningfulChanges()) {
                if (session.isEnderChest()) {
                    offlineInventoryService.saveEnderChest(session.getTarget(), session.getInventory());
                } else {
                    offlineInventoryService.save(session.getTarget(), session.getInventory());
                }
            }

            logService.logSession(session);
        } catch (Exception ex) {
            viewer.sendMessage(plugin.lang().formatMessage(
                    "invsee.save-failed",
                    "&cKon wijzigingen niet opslaan: &7{error}",
                    Map.of("error", ex.getMessage() == null ? "-" : ex.getMessage())
            ));
            plugin.getLogger().warning("Failed to save session for " + session.getTarget().getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            unlockOfflineTarget(session);
        }
    }

    public void handleTargetJoinWhileOfflineEditing(Player joinedPlayer) {
        if (joinedPlayer == null) {
            return;
        }

        UUID targetUuid = joinedPlayer.getUniqueId();
        UUID viewerUuid = offlineTargetEditors.get(targetUuid);

        if (viewerUuid == null) {
            return;
        }

        InvseeSession session = sessions.get(viewerUuid);
        if (session == null) {
            offlineTargetEditors.remove(targetUuid);
            return;
        }

        if (session.isOnlineTarget()) {
            offlineTargetEditors.remove(targetUuid);
            return;
        }

        try {
            session.finalizeSession();

            if (!session.isReadOnly() && session.hasMeaningfulChanges()) {
                if (session.isEnderChest()) {
                    offlineInventoryService.saveEnderChest(session.getTarget(), session.getInventory());
                } else {
                    offlineInventoryService.save(session.getTarget(), session.getInventory());
                }
            }

            logService.logSession(session);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to flush offline invsee session before join for " + joinedPlayer.getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            sessions.remove(viewerUuid);
            stopLiveSync(viewerUuid);
            offlineTargetEditors.remove(targetUuid);

            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer != null && viewer.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, viewer::closeInventory);
            }
        }
    }

    public boolean isInvseeInventory(Player viewer, Inventory inventory) {
        InvseeSession session = sessions.get(viewer.getUniqueId());
        return session != null && session.getInventory().equals(inventory);
    }

    public boolean isEditableSlot(int slot) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        Set<Integer> editable = new HashSet<>();
        for (int i = 0; i <= 35; i++) {
            editable.add(i);
        }

        editable.add(config.getInvseeHelmetSlot());
        editable.add(config.getInvseeChestplateSlot());
        editable.add(config.getInvseeLeggingsSlot());
        editable.add(config.getInvseeBootsSlot());
        editable.add(config.getInvseeOffhandSlot());

        return editable.contains(slot);
    }

    public boolean isProtectedTopSlot(Player viewer, int rawSlot) {
        InvseeSession session = sessions.get(viewer.getUniqueId());
        if (session == null) {
            return false;
        }

        if (session.isEnderChest()) {
            return false;
        }

        return rawSlot >= 0 && rawSlot < 54 && !isEditableSlot(rawSlot);
    }

    public boolean isProtectedTopSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < 54 && !isEditableSlot(rawSlot);
    }

    public void scheduleOnlineSync(Player viewer) {
        InvseeSession session = sessions.get(viewer.getUniqueId());
        if (session == null || !session.isOnlineTarget()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (session.isEnderChest()) {
                pushGuiToTargetEnderChest(viewer);
                refreshEnderChestFromTarget(viewer);
            } else {
                pushGuiToTarget(viewer);
                refreshFromTarget(viewer);
            }
        });
    }

    public void refreshFromTarget(Player viewer) {
        InvseeSession session = sessions.get(viewer.getUniqueId());
        if (session == null || !session.isOnlineTarget() || session.isEnderChest()) {
            return;
        }

        Player target = Bukkit.getPlayer(session.getTarget().getUniqueId());
        if (target == null || !target.isOnline()) {
            return;
        }

        Inventory gui = session.getInventory();
        PlayerInventory targetInv = target.getInventory();

        for (int i = 0; i < 36; i++) {
            gui.setItem(i, cloneItem(targetInv.getItem(i)));
        }

        MainConfig config = plugin.getConfigManager().getMainConfig();
        gui.setItem(config.getInvseeHelmetSlot(), cloneItem(targetInv.getHelmet()));
        gui.setItem(config.getInvseeChestplateSlot(), cloneItem(targetInv.getChestplate()));
        gui.setItem(config.getInvseeLeggingsSlot(), cloneItem(targetInv.getLeggings()));
        gui.setItem(config.getInvseeBootsSlot(), cloneItem(targetInv.getBoots()));
        gui.setItem(config.getInvseeOffhandSlot(), cloneItem(targetInv.getItemInOffHand()));

        placeLabelPanes(gui);
        fillDecor(gui);

        InventoryView openView = viewer.getOpenInventory();
        if (openView != null && openView.getTopInventory().equals(gui)) {
            viewer.updateInventory();
        }
    }

    public void refreshEnderChestFromTarget(Player viewer) {
        InvseeSession session = sessions.get(viewer.getUniqueId());
        if (session == null || !session.isOnlineTarget() || !session.isEnderChest()) {
            return;
        }

        Player target = Bukkit.getPlayer(session.getTarget().getUniqueId());
        if (target == null || !target.isOnline()) {
            return;
        }

        Inventory gui = session.getInventory();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, cloneItem(target.getEnderChest().getItem(i)));
        }

        InventoryView openView = viewer.getOpenInventory();
        if (openView != null && openView.getTopInventory().equals(gui)) {
            viewer.updateInventory();
        }
    }

    public void pushGuiToTarget(Player viewer) {
        InvseeSession session = sessions.get(viewer.getUniqueId());
        if (session == null || !session.isOnlineTarget() || session.isReadOnly() || session.isEnderChest()) {
            return;
        }

        Player target = Bukkit.getPlayer(session.getTarget().getUniqueId());
        if (target == null || !target.isOnline()) {
            return;
        }

        Inventory gui = session.getInventory();
        PlayerInventory targetInv = target.getInventory();

        for (int i = 0; i < 36; i++) {
            targetInv.setItem(i, cloneItem(gui.getItem(i)));
        }

        MainConfig config = plugin.getConfigManager().getMainConfig();
        targetInv.setHelmet(cloneItem(gui.getItem(config.getInvseeHelmetSlot())));
        targetInv.setChestplate(cloneItem(gui.getItem(config.getInvseeChestplateSlot())));
        targetInv.setLeggings(cloneItem(gui.getItem(config.getInvseeLeggingsSlot())));
        targetInv.setBoots(cloneItem(gui.getItem(config.getInvseeBootsSlot())));
        targetInv.setItemInOffHand(cloneItem(gui.getItem(config.getInvseeOffhandSlot())));

        target.updateInventory();
    }

    public void pushGuiToTargetEnderChest(Player viewer) {
        InvseeSession session = sessions.get(viewer.getUniqueId());
        if (session == null || !session.isOnlineTarget() || session.isReadOnly() || !session.isEnderChest()) {
            return;
        }

        Player target = Bukkit.getPlayer(session.getTarget().getUniqueId());
        if (target == null || !target.isOnline()) {
            return;
        }

        Inventory gui = session.getInventory();
        for (int i = 0; i < 27; i++) {
            target.getEnderChest().setItem(i, cloneItem(gui.getItem(i)));
        }
    }

    private boolean tryLockOfflineTarget(Player viewer, OfflinePlayer target) {
        UUID existing = offlineTargetEditors.putIfAbsent(target.getUniqueId(), viewer.getUniqueId());
        return existing == null || existing.equals(viewer.getUniqueId());
    }

    private void unlockOfflineTarget(InvseeSession session) {
        if (session == null || session.isOnlineTarget()) {
            return;
        }

        UUID targetUuid = session.getTarget().getUniqueId();
        UUID viewerUuid = session.getViewerUuid();

        offlineTargetEditors.computeIfPresent(targetUuid, (key, value) -> value.equals(viewerUuid) ? null : value);
    }

    private void startLiveSync(Player viewer, Player target, boolean enderChest) {
        stopLiveSync(viewer.getUniqueId());

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Player currentViewer = Bukkit.getPlayer(viewer.getUniqueId());
            if (currentViewer == null || !currentViewer.isOnline()) {
                stopLiveSync(viewer.getUniqueId());
                return;
            }

            InvseeSession session = sessions.get(viewer.getUniqueId());
            if (session == null || !session.isOnlineTarget()) {
                stopLiveSync(viewer.getUniqueId());
                return;
            }

            InventoryView openView = currentViewer.getOpenInventory();
            if (openView == null || !openView.getTopInventory().equals(session.getInventory())) {
                stopLiveSync(viewer.getUniqueId());
                return;
            }

            Player currentTarget = Bukkit.getPlayer(target.getUniqueId());
            if (currentTarget == null || !currentTarget.isOnline()) {
                return;
            }

            if (enderChest) {
                refreshEnderChestFromTarget(currentViewer);
            } else {
                refreshFromTarget(currentViewer);
            }
        }, 1L, 2L);

        liveSyncTasks.put(viewer.getUniqueId(), taskId);
    }

    private void stopLiveSync(UUID viewerUuid) {
        Integer taskId = liveSyncTasks.remove(viewerUuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private Inventory buildInventory(InvseeTargetData data) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        String rawTitle = config.getInvseeTitle().replace("{player}", data.getTargetName());
        String title = StringUtil.colorize(rawTitle);

        Inventory gui = Bukkit.createInventory(null, 54, title);

        for (int i = 0; i < 36 && i < data.getContents().length; i++) {
            gui.setItem(i, cloneItem(data.getContents()[i]));
        }

        if (data.getArmorContents().length >= 4) {
            gui.setItem(config.getInvseeHelmetSlot(), cloneItem(data.getArmorContents()[3]));
            gui.setItem(config.getInvseeChestplateSlot(), cloneItem(data.getArmorContents()[2]));
            gui.setItem(config.getInvseeLeggingsSlot(), cloneItem(data.getArmorContents()[1]));
            gui.setItem(config.getInvseeBootsSlot(), cloneItem(data.getArmorContents()[0]));
        }

        gui.setItem(config.getInvseeOffhandSlot(), cloneItem(data.getOffhand()));

        placeLabelPanes(gui);
        fillDecor(gui);
        return gui;
    }

    private void placeLabelPanes(Inventory gui) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        gui.setItem(config.getInvseeHelmetLabelSlot(), createLabelPane(config.getInvseeEquipmentLabelHelmet()));
        gui.setItem(config.getInvseeChestplateLabelSlot(), createLabelPane(config.getInvseeEquipmentLabelChestplate()));
        gui.setItem(config.getInvseeLeggingsLabelSlot(), createLabelPane(config.getInvseeEquipmentLabelLeggings()));
        gui.setItem(config.getInvseeBootsLabelSlot(), createLabelPane(config.getInvseeEquipmentLabelBoots()));
        gui.setItem(config.getInvseeOffhandLabelSlot(), createLabelPane(config.getInvseeEquipmentLabelOffhand()));
    }

    private String buildEnderChestTitle(String playerName) {
        String safeName = playerName == null ? "-" : playerName;
        String rawTitle = plugin.getConfigManager().getMainConfig().getEnderchestTitle().replace("{player}", safeName);
        return StringUtil.colorize(rawTitle);
    }

    private ItemStack createLabelPane(String name) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(StringUtil.colorize(name));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private void fillDecor(Inventory gui) {
        MainConfig config = plugin.getConfigManager().getMainConfig();

        Set<Integer> reserved = new HashSet<>();
        for (int i = 0; i <= 35; i++) {
            reserved.add(i);
        }

        reserved.add(config.getInvseeHelmetSlot());
        reserved.add(config.getInvseeChestplateSlot());
        reserved.add(config.getInvseeLeggingsSlot());
        reserved.add(config.getInvseeBootsSlot());
        reserved.add(config.getInvseeOffhandSlot());

        reserved.add(config.getInvseeHelmetLabelSlot());
        reserved.add(config.getInvseeChestplateLabelSlot());
        reserved.add(config.getInvseeLeggingsLabelSlot());
        reserved.add(config.getInvseeBootsLabelSlot());
        reserved.add(config.getInvseeOffhandLabelSlot());

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            pane.setItemMeta(meta);
        }

        for (int slot = 36; slot <= 53; slot++) {
            if (reserved.contains(slot)) {
                continue;
            }

            if (gui.getItem(slot) == null) {
                gui.setItem(slot, pane.clone());
            }
        }
    }
}