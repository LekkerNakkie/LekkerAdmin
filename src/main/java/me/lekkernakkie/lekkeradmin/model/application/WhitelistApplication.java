package me.lekkernakkie.lekkeradmin.model.application;

import java.util.ArrayList;
import java.util.List;

public class WhitelistApplication {

    private long id;
    private String applicationId;
    private String discordUserId;
    private String discordTag;
    private String minecraftName;
    private String minecraftUuid;
    private ApplicationStatus status;
    private String reviewReason;
    private String reviewedByDiscordId;
    private String reviewedByDiscordName;
    private long submittedAt;
    private Long reviewedAt;
    private Long linkedAt;
    private Long finalizedAt;
    private int nameRetryCount;
    private String formAnswersJson;
    private List<ApplicationAnswer> answers = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
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

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public void setReviewReason(String reviewReason) {
        this.reviewReason = reviewReason;
    }

    public String getReviewedByDiscordId() {
        return reviewedByDiscordId;
    }

    public void setReviewedByDiscordId(String reviewedByDiscordId) {
        this.reviewedByDiscordId = reviewedByDiscordId;
    }

    public String getReviewedByDiscordName() {
        return reviewedByDiscordName;
    }

    public void setReviewedByDiscordName(String reviewedByDiscordName) {
        this.reviewedByDiscordName = reviewedByDiscordName;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Long getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Long reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Long linkedAt) {
        this.linkedAt = linkedAt;
    }

    public Long getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(Long finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public int getNameRetryCount() {
        return nameRetryCount;
    }

    public void setNameRetryCount(int nameRetryCount) {
        this.nameRetryCount = nameRetryCount;
    }

    public String getFormAnswersJson() {
        return formAnswersJson;
    }

    public void setFormAnswersJson(String formAnswersJson) {
        this.formAnswersJson = formAnswersJson;
    }

    public List<ApplicationAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<ApplicationAnswer> answers) {
        this.answers = answers == null ? new ArrayList<>() : answers;
    }
}