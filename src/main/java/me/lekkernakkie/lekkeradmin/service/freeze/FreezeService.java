package me.lekkernakkie.lekkeradmin.service.freeze;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.model.freeze.FrozenPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeService {

    private final LekkerAdmin plugin;
    private final Map<UUID, FrozenPlayer> frozenPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    public FreezeService(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("freeze.enabled", true);
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.containsKey(uuid);
    }

    public boolean isFrozen(Player player) {
        return isFrozen(player.getUniqueId());
    }

    public FrozenPlayer getFrozen(UUID uuid) {
        return frozenPlayers.get(uuid);
    }

    public FrozenPlayer getFrozen(Player player) {
        return getFrozen(player.getUniqueId());
    }

    public Collection<FrozenPlayer> getFrozenPlayers() {
        return Collections.unmodifiableCollection(frozenPlayers.values());
    }

    public boolean freeze(Player actor, Player target, String reason) {
        if (!isEnabled() || target == null) {
            return false;
        }

        if (isFrozen(target)) {
            return false;
        }

        UUID actorUuid = actor != null ? actor.getUniqueId() : null;
        String actorName = actor != null ? actor.getName() : "Console";

        FrozenPlayer frozenPlayer = new FrozenPlayer(
                target.getUniqueId(),
                target.getName(),
                actorUuid,
                actorName,
                reason == null || reason.isBlank() ? "/" : reason,
                System.currentTimeMillis()
        );

        frozenPlayers.put(target.getUniqueId(), frozenPlayer);

        if (shouldSendFreezeChat()) {
            sendFreezeMessages(target, frozenPlayer);
        }

        if (shouldShowBossBar()) {
            showBossBar(target);
        }

        logFreeze(target, frozenPlayer);
        plugin.debug("Player frozen: " + target.getName() + " by " + actorName + " reason=" + frozenPlayer.getReason());
        return true;
    }

    public boolean unfreeze(Player actor, Player target) {
        if (target == null) {
            return false;
        }

        FrozenPlayer removed = frozenPlayers.remove(target.getUniqueId());
        if (removed == null) {
            return false;
        }

        hideBossBar(target);

        if (shouldSendUnfreezeChat()) {
            target.sendMessage(plugin.lang().message(
                    "freeze.unfrozen-chat",
                    "{prefix} &aJe bent niet langer gefreezed."
            ));
        }

        logUnfreeze(target, removed, actor);
        plugin.debug("Player unfrozen: " + target.getName());
        return true;
    }

    public void restoreOnJoin(Player player) {
        if (!isFrozen(player)) {
            return;
        }

        if (shouldShowBossBar()) {
            showBossBar(player);
        }

        player.sendMessage(plugin.lang().message(
                "freeze.rejoin-still-frozen",
                "{prefix} &cJe bent nog steeds gefreezed."
        ));

        logRejoinRestore(player);
    }

    public void handleQuit(Player player) {
        if (!isFrozen(player)) {
            return;
        }

        hideBossBar(player);

        if (plugin.getConfig().getBoolean("freeze.quit.log", true)) {
            logQuitWhileFrozen(player);
        }

        if (plugin.getConfig().getBoolean("freeze.quit.notify-staff", true)) {
            String msg = plugin.lang().format(
                    "freeze.quit-while-frozen",
                    "{prefix} &cSpeler &b{player} &cheeft de server verlaten tijdens freeze.",
                    Map.of("player", player.getName())
            );

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("lekkeradmin.freeze.notify") || p.hasPermission("lekkeradmin.admin"))
                    .forEach(p -> p.sendMessage(msg));
        }

        if (!plugin.getConfig().getBoolean("freeze.quit.keep-frozen-on-rejoin", true)) {
            frozenPlayers.remove(player.getUniqueId());
        }
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(bossBars.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                hideBossBar(player);
            }
        }
        bossBars.clear();
    }

    public boolean isMovementBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.movement", true);
    }

    public boolean isRotationOnlyAllowed() {
        return plugin.getConfig().getBoolean("freeze.block.rotation-only-allowed", true);
    }

    public boolean isCommandsBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.commands", true);
    }

    public boolean isDropBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.drop-items", true);
    }

    public boolean isPickupBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.pickup-items", true);
    }

    public boolean isInventoryBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.inventory-click", true);
    }

    public boolean isInteractBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.interact", true);
    }

    public boolean isBlockBreakBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.block-break", true);
    }

    public boolean isBlockPlaceBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.block-place", true);
    }

    public boolean isDamageDealBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.damage-deal", true);
    }

    public boolean isDamageTakeBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.damage-take", false);
    }

    public boolean isTeleportBlocked() {
        return plugin.getConfig().getBoolean("freeze.block.teleport", true);
    }

    public boolean isCommandAllowed(String rawCommand) {
        List<String> whitelist = plugin.getConfig().getStringList("freeze.commands-whitelist");
        if (rawCommand == null || rawCommand.isBlank()) {
            return false;
        }

        String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        String base = command.split(" ")[0].toLowerCase(Locale.ROOT);

        for (String allowed : whitelist) {
            if (allowed != null && base.equalsIgnoreCase(allowed.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldSendFreezeChat() {
        return plugin.getConfig().getBoolean("freeze.messages.send-chat-on-freeze", true);
    }

    private boolean shouldSendUnfreezeChat() {
        return plugin.getConfig().getBoolean("freeze.messages.send-chat-on-unfreeze", true);
    }

    private boolean shouldShowBossBar() {
        return plugin.getConfig().getBoolean("freeze.bossbar.enabled", true)
                && plugin.getConfig().getBoolean("freeze.messages.send-bossbar-on-freeze", true);
    }

    private void sendFreezeMessages(Player target, FrozenPlayer frozenPlayer) {
        target.sendMessage(plugin.lang().message(
                "freeze.frozen-chat",
                "{prefix} &cJe bent gefreezed door staff."
        ));

        target.sendMessage(plugin.lang().format(
                "freeze.frozen-chat-reason",
                "{prefix} &7Reden: &f{reason}",
                Map.of("reason", frozenPlayer.getReason())
        ));

        target.sendMessage(plugin.lang().message(
                "freeze.frozen-chat-instructions",
                "{prefix} &7Blijf stilstaan en wacht op een stafflid."
        ));
    }

    private void showBossBar(Player player) {
        hideBossBar(player);

        String title = plugin.getConfig().getString("freeze.bossbar.title", "&cJe bent gefreezed &7- &fWacht op staff");
        String colorName = plugin.getConfig().getString("freeze.bossbar.color", "RED");
        String styleName = plugin.getConfig().getString("freeze.bossbar.style", "SOLID");
        float progress = (float) plugin.getConfig().getDouble("freeze.bossbar.progress", 1.0D);

        BossBar.Color color = parseColor(colorName);
        BossBar.Overlay overlay = parseOverlay(styleName);

        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(title);
        BossBar bossBar = BossBar.bossBar(component, Math.max(0F, Math.min(1F, progress)), color, overlay);

        player.showBossBar(bossBar);
        bossBars.put(player.getUniqueId(), bossBar);
    }

    private void hideBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private BossBar.Color parseColor(String name) {
        try {
            return BossBar.Color.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return BossBar.Color.RED;
        }
    }

    private BossBar.Overlay parseOverlay(String name) {
        try {
            return BossBar.Overlay.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private void logFreeze(Player target, FrozenPlayer frozenPlayer) {
        if (!plugin.getConfig().getBoolean("freeze.enabled", true)) {
            return;
        }

        plugin.getLogger().info("[Freeze] " + frozenPlayer.getActorName() + " froze " + target.getName() + " reason=" + frozenPlayer.getReason());
    }

    private void logUnfreeze(Player target, FrozenPlayer frozenPlayer, Player actor) {
        String actorName = actor != null ? actor.getName() : "Console";
        plugin.getLogger().info("[Freeze] " + actorName + " unfroze " + target.getName());
    }

    private void logQuitWhileFrozen(Player player) {
        plugin.getLogger().warning("[Freeze] " + player.getName() + " quit while frozen");
    }

    private void logRejoinRestore(Player player) {
        plugin.getLogger().info("[Freeze] Restored frozen state for " + player.getName());
    }

    public String formatLocation(Location location) {
        if (location == null) {
            return "-";
        }
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    public String getGamemode(Player player) {
        GameMode gameMode = player.getGameMode();
        return gameMode != null ? gameMode.name() : "-";
    }
}