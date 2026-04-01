package me.lekkernakkie.lekkeradmin.model.link;

public class DiscordMinecraftLink {

    private long id;
    private String discordUserId;
    private String discordTag;
    private String minecraftUuid;
    private String minecraftName;
    private String applicationId;
    private Long linkedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDiscordUserId() {
        return discordUserId;
    }

    public void setDiscordUserId(String discordUserId) {
        this.discordUserId = discordUserId;
    }

    public String getDiscordTag() {
        return discordTag;
    }

    public void setDiscordTag(String discordTag) {
        this.discordTag = discordTag;
    }

    public String getMinecraftUuid() {
        return minecraftUuid;
    }

    public void setMinecraftUuid(String minecraftUuid) {
        this.minecraftUuid = minecraftUuid;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    public void setMinecraftName(String minecraftName) {
        this.minecraftName = minecraftName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getLinkedByApplicationId() {
        return applicationId;
    }

    public void setLinkedByApplicationId(String linkedByApplicationId) {
        this.applicationId = linkedByApplicationId;
    }

    public Long getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Long linkedAt) {
        this.linkedAt = linkedAt;
    }
}