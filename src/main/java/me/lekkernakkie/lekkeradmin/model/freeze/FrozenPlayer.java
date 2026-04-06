package me.lekkernakkie.lekkeradmin.model.freeze;

import java.util.UUID;

public class FrozenPlayer {

    private final UUID targetUuid;
    private final String targetName;
    private final UUID actorUuid;
    private final String actorName;
    private final String reason;
    private final long frozenAt;

    public FrozenPlayer(UUID targetUuid, String targetName, UUID actorUuid, String actorName, String reason, long frozenAt) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.actorUuid = actorUuid;
        this.actorName = actorName;
        this.reason = reason;
        this.frozenAt = frozenAt;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getActorUuid() {
        return actorUuid;
    }

    public String getActorName() {
        return actorName;
    }

    public String getReason() {
        return reason;
    }

    public long getFrozenAt() {
        return frozenAt;
    }
}