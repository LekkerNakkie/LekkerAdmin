package me.lekkernakkie.lekkeradmin.service.log;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExplosionTrackerService {

    private static final long TNT_IGNITE_LINK_MS = 5000L;
    private static final long CREEPER_TARGET_LINK_MS = 15000L;
    private static final long BLOCK_INTERACTION_LINK_MS = 8000L;
    private static final long CHAIN_WINDOW_MS = 4000L;
    private static final long RECENT_WINDOW_MS = 30000L;

    private final LekkerAdmin plugin;

    private final Map<UUID, TimedPlayerRef> trackedCreepers = new ConcurrentHashMap<>();
    private final Map<String, TimedPlayerRef> primedTntByLocationKey = new ConcurrentHashMap<>();
    private final Map<String, TimedPlayerRef> interactedBlocksByLocationKey = new ConcurrentHashMap<>();
    private final Map<String, Integer> chainCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> chainLastSeen = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> recentExplosionsByActor = new ConcurrentHashMap<>();

    public ExplosionTrackerService(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void trackCreeperTarget(Creeper creeper, Player player) {
        if (creeper == null || player == null) {
            return;
        }

        trackedCreepers.put(creeper.getUniqueId(), new TimedPlayerRef(player.getName(), System.currentTimeMillis()));
    }

    public String resolveCreeperTrigger(Creeper creeper) {
        if (creeper == null) {
            return "-";
        }

        TimedPlayerRef ref = trackedCreepers.get(creeper.getUniqueId());
        if (ref == null) {
            return "-";
        }

        if (System.currentTimeMillis() - ref.timestamp > CREEPER_TARGET_LINK_MS) {
            trackedCreepers.remove(creeper.getUniqueId());
            return "-";
        }

        return ref.playerName;
    }

    public void trackTntIgnite(Location location, Player player) {
        if (location == null || player == null) {
            return;
        }

        primedTntByLocationKey.put(toLocationKey(location), new TimedPlayerRef(player.getName(), System.currentTimeMillis()));
    }

    public String resolveTntTrigger(TNTPrimed tnt) {
        if (tnt == null) {
            return "-";
        }

        if (tnt.getSource() instanceof Player player) {
            return player.getName();
        }

        TimedPlayerRef ref = primedTntByLocationKey.get(toLocationKey(tnt.getLocation()));
        if (ref == null) {
            return "-";
        }

        if (System.currentTimeMillis() - ref.timestamp > TNT_IGNITE_LINK_MS) {
            primedTntByLocationKey.remove(toLocationKey(tnt.getLocation()));
            return "-";
        }

        return ref.playerName;
    }

    public void trackBlockInteraction(Location location, Player player) {
        if (location == null || player == null) {
            return;
        }

        interactedBlocksByLocationKey.put(toLocationKey(location), new TimedPlayerRef(player.getName(), System.currentTimeMillis()));
    }

    public String resolveBlockTrigger(Location location) {
        if (location == null) {
            return "-";
        }

        TimedPlayerRef ref = interactedBlocksByLocationKey.get(toLocationKey(location));
        if (ref == null) {
            return "-";
        }

        if (System.currentTimeMillis() - ref.timestamp > BLOCK_INTERACTION_LINK_MS) {
            interactedBlocksByLocationKey.remove(toLocationKey(location));
            return "-";
        }

        return ref.playerName;
    }

    public int registerExplosion(String actor, Location location, String type) {
        long now = System.currentTimeMillis();
        String key = buildChainKey(actor, location, type);

        Long last = chainLastSeen.get(key);
        if (last == null || (now - last) > CHAIN_WINDOW_MS) {
            chainCounters.put(key, 1);
        } else {
            chainCounters.put(key, chainCounters.getOrDefault(key, 0) + 1);
        }

        chainLastSeen.put(key, now);

        if (actor != null && !actor.isBlank() && !actor.equals("-")) {
            Deque<Long> deque = recentExplosionsByActor.computeIfAbsent(actor.toLowerCase(), ignored -> new ArrayDeque<>());
            deque.addLast(now);

            while (!deque.isEmpty() && now - deque.peekFirst() > RECENT_WINDOW_MS) {
                deque.removeFirst();
            }
        }

        return chainCounters.getOrDefault(key, 1);
    }

    public int getRecentExplosionCount(String actor) {
        if (actor == null || actor.isBlank() || actor.equals("-")) {
            return 0;
        }

        Deque<Long> deque = recentExplosionsByActor.get(actor.toLowerCase());
        if (deque == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        while (!deque.isEmpty() && now - deque.peekFirst() > RECENT_WINDOW_MS) {
            deque.removeFirst();
        }

        return deque.size();
    }

    private String buildChainKey(String actor, Location location, String type) {
        String world = location != null && location.getWorld() != null ? location.getWorld().getName() : "-";
        int x = location == null ? 0 : location.getBlockX() >> 4;
        int z = location == null ? 0 : location.getBlockZ() >> 4;
        String safeActor = actor == null || actor.isBlank() ? "-" : actor;
        String safeType = type == null || type.isBlank() ? "UNKNOWN" : type;

        return safeActor.toLowerCase() + "|" + safeType + "|" + world.toLowerCase() + "|" + x + "|" + z;
    }

    private String toLocationKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "-";
        }

        return location.getWorld().getName().toLowerCase()
                + ":" + location.getBlockX()
                + ":" + location.getBlockY()
                + ":" + location.getBlockZ();
    }

    private static final class TimedPlayerRef {
        private final String playerName;
        private final long timestamp;

        private TimedPlayerRef(String playerName, long timestamp) {
            this.playerName = playerName;
            this.timestamp = timestamp;
        }
    }
}