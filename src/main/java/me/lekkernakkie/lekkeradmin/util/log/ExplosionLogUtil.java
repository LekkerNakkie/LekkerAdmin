package me.lekkernakkie.lekkeradmin.util.log;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExplosionLogUtil {

    private ExplosionLogUtil() {
    }

    public static String buildDestroyedBlocksSummary(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "Geen blocks geraakt.";
        }

        Map<Material, Integer> counts = new LinkedHashMap<>();

        for (Block block : blocks) {
            if (block == null || block.getType().isAir()) {
                continue;
            }
            counts.merge(block.getType(), 1, Integer::sum);
        }

        if (counts.isEmpty()) {
            return "Geen blocks geraakt.";
        }

        List<String> lines = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
            lines.add("• " + entry.getKey().name() + " x" + entry.getValue());
        }

        return String.join("\n", lines);
    }

    public static String buildContainerSummary(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }

        Map<Material, Integer> counts = new LinkedHashMap<>();

        for (Block block : blocks) {
            if (block == null) {
                continue;
            }

            Material type = block.getType();
            if (!isContainerLike(type)) {
                continue;
            }

            counts.merge(type, 1, Integer::sum);
        }

        if (counts.isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
            lines.add("• " + entry.getKey().name() + " x" + entry.getValue());
        }

        return String.join("\n", lines);
    }

    public static boolean hasContainerHit(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return false;
        }

        for (Block block : blocks) {
            if (block != null && isContainerLike(block.getType())) {
                return true;
            }
        }

        return false;
    }

    public static String resolveRegionName(Location location) {
        return "-";
    }

    public static String buildAlertSummary(boolean containerHit, int chainSize, int recentExplosions, int chainThreshold, int recentThreshold) {
        List<String> alerts = new ArrayList<>();

        if (containerHit) {
            alerts.add("Container geraakt");
        }

        if (chainSize >= chainThreshold) {
            alerts.add("TNT chain x" + chainSize);
        }

        if (recentExplosions >= recentThreshold) {
            alerts.add("Veel explosions in korte tijd (" + recentExplosions + ")");
        }

        return alerts.isEmpty() ? "" : String.join("\n", alerts);
    }

    public static boolean isContainerLike(Material material) {
        if (material == null) {
            return false;
        }

        String name = material.name();

        return name.endsWith("CHEST")
                || name.endsWith("BARREL")
                || name.contains("SHULKER_BOX")
                || name.equals("HOPPER")
                || name.equals("FURNACE")
                || name.equals("BLAST_FURNACE")
                || name.equals("SMOKER")
                || name.equals("DROPPER")
                || name.equals("DISPENSER")
                || name.equals("BREWING_STAND");
    }
}