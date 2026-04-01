package me.lekkernakkie.lekkeradmin.cache;

import me.lekkernakkie.lekkeradmin.model.link.DiscordMinecraftLink;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordLinkCache {

    private final Map<String, DiscordMinecraftLink> byDiscordId = new ConcurrentHashMap<>();
    private final Map<String, DiscordMinecraftLink> byMinecraftName = new ConcurrentHashMap<>();

    public void put(DiscordMinecraftLink link) {
        if (link == null) {
            return;
        }

        if (link.getDiscordUserId() != null && !link.getDiscordUserId().isBlank()) {
            byDiscordId.put(link.getDiscordUserId(), link);
        }

        if (link.getMinecraftName() != null && !link.getMinecraftName().isBlank()) {
            byMinecraftName.put(link.getMinecraftName().toLowerCase(), link);
        }
    }

    public Optional<DiscordMinecraftLink> getByDiscordId(String discordId) {
        if (discordId == null || discordId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byDiscordId.get(discordId));
    }

    public Optional<DiscordMinecraftLink> getByMinecraftName(String minecraftName) {
        if (minecraftName == null || minecraftName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byMinecraftName.get(minecraftName.toLowerCase()));
    }

    public void removeByDiscordId(String discordId) {
        if (discordId == null || discordId.isBlank()) {
            return;
        }

        DiscordMinecraftLink removed = byDiscordId.remove(discordId);
        if (removed != null && removed.getMinecraftName() != null) {
            byMinecraftName.remove(removed.getMinecraftName().toLowerCase());
        }
    }

    public void clear() {
        byDiscordId.clear();
        byMinecraftName.clear();
    }
}