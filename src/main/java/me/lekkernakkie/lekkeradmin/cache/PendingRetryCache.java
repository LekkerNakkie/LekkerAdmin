package me.lekkernakkie.lekkeradmin.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PendingRetryCache {

    private final Map<String, String> discordUserToApplicationId = new ConcurrentHashMap<>();

    public void put(String discordUserId, String applicationId) {
        if (discordUserId == null || discordUserId.isBlank() || applicationId == null || applicationId.isBlank()) {
            return;
        }
        discordUserToApplicationId.put(discordUserId, applicationId);
    }

    public Optional<String> getApplicationId(String discordUserId) {
        if (discordUserId == null || discordUserId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(discordUserToApplicationId.get(discordUserId));
    }

    public void remove(String discordUserId) {
        if (discordUserId != null) {
            discordUserToApplicationId.remove(discordUserId);
        }
    }

    public void clear() {
        discordUserToApplicationId.clear();
    }

    public int size() {
        return discordUserToApplicationId.size();
    }
}