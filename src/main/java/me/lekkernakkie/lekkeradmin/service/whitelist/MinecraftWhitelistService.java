package me.lekkernakkie.lekkeradmin.service.whitelist;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class MinecraftWhitelistService {

    private final LekkerAdmin plugin;

    public MinecraftWhitelistService(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public boolean addToWhitelist(String minecraftName) {
        if (minecraftName == null || minecraftName.isBlank()) {
            return false;
        }

        return callSync(() -> Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "whitelist add " + minecraftName
        ));
    }

    public boolean addToWhitelist(UUID uuid, String minecraftName) {
        if (minecraftName == null || minecraftName.isBlank()) {
            return false;
        }
        return addToWhitelist(minecraftName);
    }

    public boolean removeFromWhitelist(String minecraftName) {
        if (minecraftName == null || minecraftName.isBlank()) {
            return false;
        }

        return callSync(() -> Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "whitelist remove " + minecraftName
        ));
    }

    public boolean isWhitelisted(String minecraftName, boolean caseSensitive) {
        if (minecraftName == null || minecraftName.isBlank()) {
            return false;
        }

        return callSync(() -> {
            Set<OfflinePlayer> players = Bukkit.getWhitelistedPlayers();
            for (OfflinePlayer player : players) {
                String name = player.getName();
                if (name == null) {
                    continue;
                }

                if (caseSensitive) {
                    if (name.equals(minecraftName)) {
                        return true;
                    }
                } else {
                    if (name.equalsIgnoreCase(minecraftName)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private <T> T callSync(Callable<T> callable) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return callable.call();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to execute whitelist action on main thread.", ex);
            }
        }

        FutureTask<T> task = new FutureTask<>(callable);
        Bukkit.getScheduler().runTask(plugin, task);

        try {
            return task.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Whitelist action was interrupted.", ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException("Whitelist action failed.", ex.getCause());
        }
    }
}