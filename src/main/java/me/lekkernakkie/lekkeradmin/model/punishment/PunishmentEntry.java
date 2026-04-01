package me.lekkernakkie.lekkeradmin.model.punishment;

public class PunishmentEntry {

    private long id;

    private String discordName;
    private String discordId;

    private String minecraftName;
    private String minecraftUuid;

    private PunishmentType punishmentType;

    private String reason;

    private String issuedByName;
    private String issuedByUuid;
    private String issuedByDiscordName;
    private String issuedByDiscordId;

    private PunishmentSource issuedSource;

    private Long durationMs;

    private long issuedAt;
    private Long expiresAt;

    private PunishmentStatus status;

    private Long removedAt;

    private String removedByName;
    private String removedByUuid;
    private String removedByDiscordName;
    private String removedByDiscordId;

    private String removeReason;

    private String serverName;

    private boolean notifyOnJoin;
    private boolean notificationDelivered;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDiscordName() {
        return discordName;
    }

    public void setDiscordName(String discordName) {
        this.discordName = discordName;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    public void setMinecraftName(String minecraftName) {
        this.minecraftName = minecraftName;
    }

    public String getMinecraftUuid() {
        return minecraftUuid;
    }

    public void setMinecraftUuid(String minecraftUuid) {
        this.minecraftUuid = minecraftUuid;
    }

    public PunishmentType getPunishmentType() {
        return punishmentType;
    }

    public void setPunishmentType(PunishmentType punishmentType) {
        this.punishmentType = punishmentType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public void setIssuedByName(String issuedByName) {
        this.issuedByName = issuedByName;
    }

    public String getIssuedByUuid() {
        return issuedByUuid;
    }

    public void setIssuedByUuid(String issuedByUuid) {
        this.issuedByUuid = issuedByUuid;
    }

    public String getIssuedByDiscordName() {
        return issuedByDiscordName;
    }

    public void setIssuedByDiscordName(String issuedByDiscordName) {
        this.issuedByDiscordName = issuedByDiscordName;
    }

    public String getIssuedByDiscordId() {
        return issuedByDiscordId;
    }

    public void setIssuedByDiscordId(String issuedByDiscordId) {
        this.issuedByDiscordId = issuedByDiscordId;
    }

    public PunishmentSource getIssuedSource() {
        return issuedSource;
    }

    public void setIssuedSource(PunishmentSource issuedSource) {
        this.issuedSource = issuedSource;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public PunishmentStatus getStatus() {
        return status;
    }

    public void setStatus(PunishmentStatus status) {
        this.status = status;
    }

    public Long getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Long removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemovedByName() {
        return removedByName;
    }

    public void setRemovedByName(String removedByName) {
        this.removedByName = removedByName;
    }

    public String getRemovedByUuid() {
        return removedByUuid;
    }

    public void setRemovedByUuid(String removedByUuid) {
        this.removedByUuid = removedByUuid;
    }

    public String getRemovedByDiscordName() {
        return removedByDiscordName;
    }

    public void setRemovedByDiscordName(String removedByDiscordName) {
        this.removedByDiscordName = removedByDiscordName;
    }

    public String getRemovedByDiscordId() {
        return removedByDiscordId;
    }

    public void setRemovedByDiscordId(String removedByDiscordId) {
        this.removedByDiscordId = removedByDiscordId;
    }

    public String getRemoveReason() {
        return removeReason;
    }

    public void setRemoveReason(String removeReason) {
        this.removeReason = removeReason;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean isNotifyOnJoin() {
        return notifyOnJoin;
    }

    public void setNotifyOnJoin(boolean notifyOnJoin) {
        this.notifyOnJoin = notifyOnJoin;
    }

    public boolean isNotificationDelivered() {
        return notificationDelivered;
    }

    public void setNotificationDelivered(boolean notificationDelivered) {
        this.notificationDelivered = notificationDelivered;
    }
}