package me.lekkernakkie.lekkeradmin.cache;

import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationCache {

    private final Map<String, WhitelistApplication> byApplicationId = new ConcurrentHashMap<>();

    public void put(WhitelistApplication application) {
        if (application == null || application.getApplicationId() == null || application.getApplicationId().isBlank()) {
            return;
        }
        byApplicationId.put(application.getApplicationId(), application);
    }

    public Optional<WhitelistApplication> get(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byApplicationId.get(applicationId));
    }

    public boolean contains(String applicationId) {
        return applicationId != null && byApplicationId.containsKey(applicationId);
    }

    public void remove(String applicationId) {
        if (applicationId != null) {
            byApplicationId.remove(applicationId);
        }
    }

    public void clear() {
        byApplicationId.clear();
    }

    public int size() {
        return byApplicationId.size();
    }
}