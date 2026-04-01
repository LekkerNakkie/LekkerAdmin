package me.lekkernakkie.lekkeradmin.listener.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogTypeSettings;
import me.lekkernakkie.lekkeradmin.model.log.ExplosionLogContext;
import me.lekkernakkie.lekkeradmin.service.log.ExplosionTrackerService;
import me.lekkernakkie.lekkeradmin.util.log.ExplosionLogUtil;
import me.lekkernakkie.lekkeradmin.util.log.LocationLogUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;

public class ExplosionLogListener implements Listener {

    private static final int TNT_CHAIN_ALERT_THRESHOLD = 3;
    private static final int RECENT_EXPLOSION_ALERT_THRESHOLD = 5;

    private final LekkerAdmin plugin;
    private final ExplosionTrackerService trackerService;

    public ExplosionLogListener(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.trackerService = plugin.getExplosionTrackerService();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerIgniteTnt(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getPlayer() == null) {
            return;
        }

        Material type = event.getClickedBlock().getType();

        if (type == Material.TNT) {
            trackerService.trackTntIgnite(event.getClickedBlock().getLocation(), event.getPlayer());
            return;
        }

        if (isBed(type) || type == Material.RESPAWN_ANCHOR) {
            trackerService.trackBlockInteraction(event.getClickedBlock().getLocation(), event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getBed() == null || event.getPlayer() == null) {
            return;
        }

        trackerService.trackBlockInteraction(event.getBed().getLocation(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getBlock() == null || event.getPlayer() == null) {
            return;
        }

        Material type = event.getBlock().getType();

        if (type == Material.TNT) {
            trackerService.trackTntIgnite(event.getBlock().getLocation(), event.getPlayer());
            return;
        }

        if (isBed(type) || type == Material.RESPAWN_ANCHOR) {
            trackerService.trackBlockInteraction(event.getBlock().getLocation(), event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreeperTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) {
            return;
        }

        if (!(event.getTarget() instanceof Player player)) {
            return;
        }

        trackerService.trackCreeperTarget(creeper, player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return;
        }

        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getExplosionLogs();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity == null) {
            return;
        }

        String type = resolveType(entity);
        String actor = resolveActor(entity, type);
        Location location = entity.getLocation();
        List<Block> blocks = new ArrayList<>(event.blockList());

        dispatchExplosion(settings, type, actor, location, blocks, event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getLogsConfig() == null) {
            return;
        }

        LogTypeSettings settings = plugin.getConfigManager().getLogsConfig().getExplosionLogs();
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        Block block = event.getBlock();
        if (block == null) {
            return;
        }

        String type = resolveBlockExplosionType(block.getType());
        String actor = trackerService.resolveBlockTrigger(block.getLocation());
        Location location = block.getLocation();
        List<Block> blocks = new ArrayList<>(event.blockList());

        dispatchExplosion(settings, type, actor, location, blocks, event.isCancelled());
    }

    private void dispatchExplosion(LogTypeSettings settings, String type, String actor, Location location, List<Block> blocks, boolean cancelled) {
        int chainSize = trackerService.registerExplosion(actor, location, type);
        int recentExplosions = trackerService.getRecentExplosionCount(actor);

        boolean containerHit = ExplosionLogUtil.hasContainerHit(blocks);
        String destroyedSummary = ExplosionLogUtil.buildDestroyedBlocksSummary(blocks);
        String containerSummary = ExplosionLogUtil.buildContainerSummary(blocks);
        String alertSummary = ExplosionLogUtil.buildAlertSummary(
                containerHit,
                chainSize,
                recentExplosions,
                TNT_CHAIN_ALERT_THRESHOLD,
                RECENT_EXPLOSION_ALERT_THRESHOLD
        );

        ExplosionLogContext context = new ExplosionLogContext(
                type,
                actor,
                LocationLogUtil.formatWorld(location),
                LocationLogUtil.formatCoordinates(location),
                ExplosionLogUtil.resolveRegionName(location),
                cancelled,
                blocks == null ? 0 : blocks.size(),
                destroyedSummary,
                containerSummary,
                chainSize,
                alertSummary
        );

        plugin.getMinecraftLogDispatcher().dispatchExplosion(settings, context);
    }

    private String resolveType(Entity entity) {
        if (entity instanceof TNTPrimed) {
            return "TNT";
        }
        if (entity instanceof ExplosiveMinecart) {
            return "TNT_MINECART";
        }
        if (entity instanceof Creeper) {
            return "CREEPER";
        }
        if (entity instanceof WitherSkull) {
            return "WITHER_SKULL";
        }
        if (entity instanceof Fireball) {
            return "FIREBALL";
        }
        if (entity instanceof EnderCrystal) {
            return "END_CRYSTAL";
        }
        if (entity instanceof Projectile projectile) {
            return "PROJECTILE_" + projectile.getType().name();
        }

        return entity.getType().name();
    }

    private String resolveActor(Entity entity, String type) {
        if ("TNT".equalsIgnoreCase(type) && entity instanceof TNTPrimed tnt) {
            return trackerService.resolveTntTrigger(tnt);
        }

        if ("CREEPER".equalsIgnoreCase(type) && entity instanceof Creeper creeper) {
            return trackerService.resolveCreeperTrigger(creeper);
        }

        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player.getName();
            }
        }

        if (entity instanceof Fireball fireball) {
            ProjectileSource shooter = fireball.getShooter();
            if (shooter instanceof Player player) {
                return player.getName();
            }
        }

        if (entity instanceof WitherSkull witherSkull) {
            ProjectileSource shooter = witherSkull.getShooter();
            if (shooter instanceof Player player) {
                return player.getName();
            }
        }

        if (entity instanceof ExplosiveMinecart) {
            return "-";
        }

        return "-";
    }

    private String resolveBlockExplosionType(Material material) {
        if (material == null) {
            return "BLOCK_EXPLOSION";
        }

        if (material == Material.RESPAWN_ANCHOR) {
            return "RESPAWN_ANCHOR";
        }

        if (isBed(material)) {
            return "BED";
        }

        if (material == Material.TNT) {
            return "TNT_BLOCK";
        }

        return material.name() + "_EXPLOSION";
    }

    private boolean isBed(Material material) {
        if (material == null) {
            return false;
        }

        return material.name().endsWith("_BED");
    }
}