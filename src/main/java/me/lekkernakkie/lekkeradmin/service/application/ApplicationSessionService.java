package me.lekkernakkie.lekkeradmin.service.application;

import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationSessionService {

    private final Map<String, WhitelistApplication> draftSessions = new ConcurrentHashMap<>();

    public void putDraft(String discordUserId, WhitelistApplication application) {
        if (discordUserId == null || application == null) {
            return;
        }
        draftSessions.put(discordUserId, application);
    }

    public Optional<WhitelistApplication> getDraft(String discordUserId) {
        if (discordUserId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(draftSessions.get(discordUserId));
    }

    public boolean hasDraft(String discordUserId) {
        return discordUserId != null && draftSessions.containsKey(discordUserId);
    }

    public void removeDraft(String discordUserId) {
        if (discordUserId != null) {
            draftSessions.remove(discordUserId);
        }
    }

    public void clearAll() {
        draftSessions.clear();
    }

    public int size() {
        return draftSessions.size();
    }
}