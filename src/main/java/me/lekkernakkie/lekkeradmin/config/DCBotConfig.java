package me.lekkernakkie.lekkeradmin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DCBotConfig {

    private final FileConfiguration config;

    public DCBotConfig(FileConfiguration config) {
        this.config = config;
    }

    public boolean isBotEnabled() {
        return config.getBoolean("bot.enabled", true);
    }

    public String getBotToken() {
        return config.getString("bot.token", "");
    }

    public String getGuildId() {
        return config.getString("bot.guild-id", "");
    }

    public boolean isStatusEnabled() {
        return config.getBoolean("bot.status.enabled", true);
    }

    public String getStatusType() {
        return config.getString("bot.status.type", "PLAYING");
    }

    public String getStatusText() {
        return config.getString("bot.status.text", "Whitelist");
    }

    public int getStatusIntervalSeconds() {
        return config.getInt("bot.status.interval-seconds", 30);
    }

    public List<String> getStatusMessages() {
        return config.getStringList("bot.status.messages");
    }

    public String getReviewChannelId() {
        return config.getString("channels.review-channel-id", "");
    }

    public String getLogChannelId() {
        return config.getString("channels.log-channel-id", "");
    }

    public boolean isWebhookEnabled() {
        return config.getBoolean("webhooks.moderation-log.enabled", false);
    }

    public boolean isWebhookLoggingEnabled() {
        return config.getBoolean("logging.webhook.enabled", true);
    }

    public String getWebhookUrl() {
        return config.getString("webhooks.moderation-log.url", "");
    }

    public String getWebhookUsername() {
        return config.getString("webhooks.moderation-log.username", "LekkerAdmin Logs");
    }

    public String getWebhookAvatarUrl() {
        return config.getString("webhooks.moderation-log.avatar-url", "");
    }

    public boolean shouldLogApprovals() {
        return config.getBoolean("logging.webhook.log-approvals", true);
    }

    public boolean shouldLogDenials() {
        return config.getBoolean("logging.webhook.log-denials", true);
    }

    public boolean shouldLogNameFailures() {
        return config.getBoolean("logging.webhook.log-name-failures", true);
    }

    public boolean shouldLogNameRetries() {
        return config.getBoolean("logging.webhook.log-name-retries", true);
    }

    public boolean shouldLogAutoCompletions() {
        return config.getBoolean("logging.webhook.log-auto-completions", true);
    }

    public String getApprovalLogFormat() {
        return config.getString(
                "logging.formats.approval",
                "{reviewer} keurde aanvraag {application_id} goed voor {discord_user} / {minecraft_name}"
        );
    }

    public String getDenialLogFormat() {
        return config.getString(
                "logging.formats.denial",
                "{reviewer} keurde aanvraag {application_id} af voor {discord_user} / {minecraft_name}. Reden: {reason}"
        );
    }

    public String getInvalidNameLogFormat() {
        return config.getString(
                "logging.formats.invalid-name",
                "Naam validatie mislukt voor aanvraag {application_id}. Ingegeven naam: {minecraft_name}"
        );
    }

    public String getRetrySuccessLogFormat() {
        return config.getString(
                "logging.formats.retry-success",
                "Naamcorrectie succesvol voor aanvraag {application_id}. Oude naam: {old_name}, nieuwe naam: {new_name}"
        );
    }

    public String getDatabaseType() {
        return config.getString("database.type", "SQLITE");
    }

    public String getSqliteFile() {
        return config.getString("database.sqlite.file", "data.db");
    }

    public String getMysqlHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMysqlPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return config.getString("database.mysql.database", "lekkeradmin");
    }

    public String getMysqlUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMysqlPassword() {
        return config.getString("database.mysql.password", "");
    }

    public boolean isMysqlSsl() {
        return config.getBoolean("database.mysql.ssl", false);
    }

    public int getMysqlPoolSize() {
        return config.getInt("database.mysql.pool-size", 10);
    }

    public boolean isWhitelistEnabled() {
        return config.getBoolean("whitelist.enabled", true);
    }

    public boolean isAutoLinkOnApprove() {
        return config.getBoolean("whitelist.auto-link-on-approve", true);
    }

    public boolean isAutoWhitelistOnValidName() {
        return config.getBoolean("whitelist.auto-whitelist-on-valid-name", true);
    }

    public boolean isRequireReviewBeforeWhitelist() {
        return config.getBoolean("whitelist.require-review-before-whitelist", true);
    }

    public boolean isAllowNameRetryAfterApproval() {
        return config.getBoolean("whitelist.allow-name-retry-after-approval", true);
    }

    public int getMaxNameRetries() {
        return config.getInt("whitelist.max-name-retries", 3);
    }

    public boolean isNotifyStaffOnNameFailure() {
        return config.getBoolean("whitelist.notify-staff-on-name-failure", true);
    }

    public boolean isNotifyPlayerOnNameFailure() {
        return config.getBoolean("whitelist.notify-player-on-name-failure", true);
    }

    public boolean isCaseSensitiveNameCheck() {
        return config.getBoolean("whitelist.case-sensitive-name-check", false);
    }

    public boolean isAllowMultipleOpenApplications() {
        return config.getBoolean("whitelist.allow-multiple-open-applications", false);
    }

    public int getWhitelistCommandCooldownSeconds() {
        return config.getInt("whitelist.command-cooldown-seconds", 60);
    }

    public boolean isWhitelistIngameNotifyEnabled() {
        return config.getBoolean("whitelist.ingame-notify.enabled", false);
    }

    public String getWhitelistIngameNotifyPermission() {
        return config.getString("whitelist.ingame-notify.permission", "lekkeradmin.notify.whitelist");
    }

    public String getWhitelistIngameNotifyMessage() {
        return config.getString(
                "whitelist.ingame-notify.message",
                "&7[&bWhitelist&7] &fNieuwe aanvraag van &b{discord}&f voor &b{minecraft}&f."
        );
    }

    public String getWhitelistIngameNotifySound() {
        return config.getString("whitelist.ingame-notify.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    public float getWhitelistIngameNotifyVolume() {
        return (float) config.getDouble("whitelist.ingame-notify.volume", 0.7D);
    }

    public float getWhitelistIngameNotifyPitch() {
        return (float) config.getDouble("whitelist.ingame-notify.pitch", 1.4D);
    }

    public boolean isWhitelistReviewThreadEnabled() {
        return config.getBoolean("whitelist.review-thread.enabled", true);
    }

    public String getWhitelistReviewThreadNameFormat() {
        return config.getString("whitelist.review-thread.name-format", "Whitelist • {minecraft}");
    }

    public boolean isMinecraftNameValidationEnabled() {
        return config.getBoolean("validation.minecraft-name.enabled", true);
    }

    public int getMinecraftNameMinLength() {
        return config.getInt("validation.minecraft-name.min-length", 3);
    }

    public int getMinecraftNameMaxLength() {
        return config.getInt("validation.minecraft-name.max-length", 16);
    }

    public String getMinecraftNameRegex() {
        return config.getString("validation.minecraft-name.regex", "^[A-Za-z0-9_]+$");
    }

    public boolean isCheckProfileExistsEnabled() {
        return config.getBoolean("validation.minecraft-name.check-profile-exists", true);
    }

    public String getWhitelistFormTitle() {
        return config.getString("forms.whitelist.title", "Whitelist aanvraag");
    }

    public boolean isWhitelistDmMessageEnabled() {
        return config.getBoolean("forms.whitelist.dm-message.enabled", true);
    }

    public List<String> getWhitelistDmMessageLines() {
        return config.getStringList("forms.whitelist.dm-message.content");
    }

    public String getWhitelistDmMessageContent() {
        List<String> lines = getWhitelistDmMessageLines();
        if (lines == null || lines.isEmpty()) {
            return "Klik op de knop hieronder om je whitelist aanvraag te starten.";
        }
        return String.join("\n", lines);
    }

    public String getWhitelistOpenButtonText() {
        return config.getString("forms.whitelist.dm-message.button-text", "Formulier openen");
    }

    public String getConfirmMessageTitle() {
        return config.getString("forms.whitelist.confirm-message.title", "Controleer je aanvraag");
    }

    public List<String> getConfirmMessageDescription() {
        return config.getStringList("forms.whitelist.confirm-message.description");
    }

    public String getConfirmButtonText() {
        return config.getString("forms.whitelist.confirm-message.confirm-button", "Bevestigen");
    }

    public String getEditButtonText() {
        return config.getString("forms.whitelist.confirm-message.edit-button", "Opnieuw invullen");
    }

    public String getCancelButtonText() {
        return config.getString("forms.whitelist.confirm-message.cancel-button", "Annuleren");
    }

    public List<ApplicationField> getApplicationFields() {
        List<ApplicationField> fields = new ArrayList<>();

        ConfigurationSection section = config.getConfigurationSection("forms.whitelist.fields");
        if (section == null) {
            return fields;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection fieldSection = section.getConfigurationSection(key);
            if (fieldSection == null || !fieldSection.getBoolean("enabled", true)) {
                continue;
            }

            fields.add(new ApplicationField(
                    key,
                    fieldSection.getString("label", key),
                    fieldSection.getString("style", "SHORT"),
                    fieldSection.getString("placeholder", ""),
                    fieldSection.getBoolean("required", true),
                    fieldSection.getInt("min-length", 1),
                    fieldSection.getInt("max-length", 100),
                    fieldSection.getInt("order", 0)
            ));
        }

        fields.sort(Comparator.comparingInt(ApplicationField::order));
        return fields;
    }

    public String getApplicationFieldLabel(String key) {
        for (ApplicationField field : getApplicationFields()) {
            if (field.key().equalsIgnoreCase(key)) {
                return field.label();
            }
        }
        return key;
    }

    public String getNameRetryModalTitle() {
        return config.getString("name-retry.modal.title", "Minecraft naam corrigeren");
    }

    public String getNameRetryFieldLabel() {
        return config.getString("name-retry.modal.field.label", "Juiste Minecraft naam");
    }

    public String getNameRetryFieldStyle() {
        return config.getString("name-retry.modal.field.style", "SHORT");
    }

    public String getNameRetryFieldPlaceholder() {
        return config.getString("name-retry.modal.field.placeholder", "Bijv. LekkerNakkie");
    }

    public boolean isNameRetryFieldRequired() {
        return config.getBoolean("name-retry.modal.field.required", true);
    }

    public int getNameRetryFieldMinLength() {
        return config.getInt("name-retry.modal.field.min-length", 3);
    }

    public int getNameRetryFieldMaxLength() {
        return config.getInt("name-retry.modal.field.max-length", 16);
    }

    public String getSlashWhitelistStarted() {
        return config.getString("messages.slash.whitelist-started", "Ik heb je een DM gestuurd met je whitelist formulier.");
    }

    public String getSlashDmFailed() {
        return config.getString("messages.slash.dm-failed", "Ik kan je geen DM sturen. Zet je privéberichten aan en probeer opnieuw.");
    }

    public String getSuggestionSubmittedMessage() {
        return config.getString("messages.slash.suggest-submitted", "Je suggestie is ingestuurd.");
    }

    public String getSuggestionDisabledMessage() {
        return config.getString("messages.slash.suggest-disabled", "Suggesties zijn momenteel uitgeschakeld.");
    }

    public String getSuggestionMissingChannelMessage() {
        return config.getString("messages.slash.suggest-missing-channel", "Het suggestiekanaal is niet correct ingesteld.");
    }

    public String getSuggestionInvalidLengthMessage() {
        return config.getString("messages.slash.suggest-invalid-length", "Je suggestie moet tussen {min} en {max} tekens lang zijn.");
    }

    public String getDmApplicationSubmitted() {
        return config.getString("messages.dm.application-submitted", "Je whitelist aanvraag is doorgestuurd naar staff.");
    }

    public String getDmApplicationCancelled() {
        return config.getString("messages.dm.application-cancelled", "Je aanvraag is geannuleerd.");
    }

    public String getDmApplicationApproved() {
        return config.getString("messages.dm.application-approved", "Je aanvraag is goedgekeurd! Je bent toegevoegd aan de whitelist.");
    }

    public String getDmApplicationDenied() {
        return config.getString("messages.dm.application-denied", "Je aanvraag is afgekeurd.");
    }

    public String getDmApplicationDeniedWithReason() {
        return config.getString("messages.dm.application-denied-with-reason", "Je aanvraag is afgekeurd. Reden: {reason}");
    }

    public String getDmInvalidMinecraftName() {
        return config.getString("messages.dm.invalid-minecraft-name", "De Minecraft naam die je hebt opgegeven klopt niet.");
    }

    public String getDmRetryNameSuccess() {
        return config.getString("messages.dm.retry-name-success", "Je naam is correct verwerkt. Je bent nu toegevoegd aan de whitelist.");
    }

    public String getDmRetryNameFailed() {
        return config.getString("messages.dm.retry-name-failed", "Deze naam is nog steeds ongeldig. Probeer opnieuw.");
    }

    public String getDmBlockedOpenApplication() {
        return config.getString("messages.dm.blocked-open-application", "Je hebt al een lopende aanvraag. Wacht op staff of gebruik je bestaande aanvraag.");
    }

    public String getDmBlockedAlreadyWhitelisted() {
        return config.getString("messages.dm.blocked-already-whitelisted", "Je bent al gewhitelist. Je kan geen nieuwe aanvraag indienen.");
    }

    public String getDmBlockedDiscordAlreadyLinked() {
        return config.getString("messages.dm.blocked-discord-already-linked", "Je Discord account is al gekoppeld aan een Minecraft account. Je kan geen nieuwe aanvraag indienen.");
    }

    public String getDmBlockedMinecraftAlreadyLinked() {
        return config.getString("messages.dm.blocked-minecraft-already-linked", "Deze Minecraft naam is al gekoppeld aan een ander Discord account.");
    }

    public String getStaffReviewTitle() {
        return config.getString("messages.staff.review-title", "Nieuwe whitelist aanvraag");
    }

    public String getStaffApproved() {
        return config.getString("messages.staff.approved", "Aanvraag goedgekeurd.");
    }

    public String getStaffDenied() {
        return config.getString("messages.staff.denied", "Aanvraag afgekeurd.");
    }

    public String getStaffPendingNameFix() {
        return config.getString("messages.staff.pending-name-fix", "Aanvraag goedgekeurd, maar Minecraft naam is ongeldig.");
    }

    public String getStaffRetryProcessed() {
        return config.getString("messages.staff.retry-processed", "Naamcorrectie succesvol verwerkt.");
    }

    public String getEmbedWhitelistStartTitle() {
        return config.getString("embeds.texts.whitelist-start.title", "Whitelist Aanvraag");
    }

    public String getEmbedWhitelistStartDescription() {
        return config.getString("embeds.texts.whitelist-start.description", getWhitelistDmMessageContent());
    }

    public String getEmbedWhitelistStartFooter() {
        return config.getString("embeds.texts.whitelist-start.footer", "");
    }

    public String getEmbedSubmittedTitle() {
        return config.getString("embeds.texts.submitted.title", getEmbedReviewTitle());
    }

    public String getEmbedSubmittedDescription() {
        return config.getString("embeds.texts.submitted.description", getDmApplicationSubmitted());
    }

    public String getEmbedSubmittedFooter() {
        return config.getString("embeds.texts.submitted.footer", "");
    }

    public String getEmbedApprovedTitle() {
        return config.getString("embeds.texts.approved.title", "Whitelist Toegelaten");
    }

    public String getEmbedApprovedDescription() {
        return config.getString("embeds.texts.approved.description", getDmApplicationApproved());
    }

    public String getEmbedApprovedFooter() {
        return config.getString("embeds.texts.approved.footer", "");
    }

    public String getEmbedDeniedTitle() {
        return config.getString("embeds.texts.denied.title", "Whitelist Geweigerd");
    }

    public String getEmbedDeniedDescription() {
        return config.getString("embeds.texts.denied.description", getDmApplicationDenied());
    }

    public String getEmbedDeniedFooter() {
        return config.getString("embeds.texts.denied.footer", "");
    }

    public String getEmbedInvalidNameTitle() {
        return config.getString("embeds.texts.invalid-name.title", "Minecraft naam ongeldig");
    }

    public String getEmbedInvalidNameDescription() {
        return config.getString("embeds.texts.invalid-name.description", getDmInvalidMinecraftName());
    }

    public String getEmbedInvalidNameFooter() {
        return config.getString("embeds.texts.invalid-name.footer", "");
    }

    public String getEmbedReviewTitle() {
        return config.getString("embeds.texts.review.title", getStaffReviewTitle());
    }

    public String getEmbedReviewDescription() {
        return config.getString("embeds.texts.review.description", "");
    }

    public String getEmbedReviewFooter() {
        return config.getString("embeds.texts.review.footer", "");
    }

    public String getEmbedApprovedReasonFieldName() {
        return config.getString("embeds.texts.fields.reason", "Reden");
    }

    public String getEmbedMinecraftNameFieldName() {
        return config.getString("embeds.texts.fields.minecraft-name", "Minecraft naam");
    }

    public String getEmbedDiscordFieldName() {
        return config.getString("embeds.texts.fields.discord", "Discord");
    }

    public String getEmbedReviewerFieldName() {
        return config.getString("embeds.texts.fields.reviewer", "Reviewer");
    }

    public String getEmbedProblemFieldName() {
        return config.getString("embeds.texts.fields.problem", "Probleem");
    }

    public String getApproveButtonText() {
        return config.getString("buttons.approve.text", "Goedkeuren");
    }

    public String getApproveButtonStyle() {
        return config.getString("buttons.approve.style", "SUCCESS");
    }

    public String getApproveWithReasonButtonText() {
        return config.getString("buttons.approve-reason.text", "Goedkeuren met reden");
    }

    public String getApproveWithReasonButtonStyle() {
        return config.getString("buttons.approve-reason.style", "PRIMARY");
    }

    public String getDenyButtonText() {
        return config.getString("buttons.deny.text", "Afkeuren");
    }

    public String getDenyButtonStyle() {
        return config.getString("buttons.deny.style", "DANGER");
    }

    public String getDenyWithReasonButtonText() {
        return config.getString("buttons.deny-reason.text", "Afkeuren met reden");
    }

    public String getDenyWithReasonButtonStyle() {
        return config.getString("buttons.deny-reason.style", "SECONDARY");
    }

    public String getRetryNameButtonText() {
        return config.getString("buttons.retry-name.text", "Naam opnieuw ingeven");
    }

    public String getRetryNameButtonStyle() {
        return config.getString("buttons.retry-name.style", "PRIMARY");
    }

    public String getWhitelistStartButtonStyle() {
        return config.getString("buttons.open-form.style", "PRIMARY");
    }

    public int getReviewEmbedColor() {
        return parseColor("embeds.colors.review", "#00C8FF");
    }

    public int getApprovedEmbedColor() {
        return parseColor("embeds.colors.approved", "#33CC66");
    }

    public int getDeniedEmbedColor() {
        return parseColor("embeds.colors.denied", "#FF4D4D");
    }

    public int getPendingFixEmbedColor() {
        return parseColor("embeds.colors.pending-fix", "#FFB347");
    }

    public int getLogEmbedColor() {
        return parseColor("embeds.colors.log", "#7289DA");
    }

    public List<String> getReviewRoleIds() {
        return config.getStringList("roles.review-role-ids");
    }

    public boolean isGiveRoleOnApproveEnabled() {
        return config.getBoolean("roles.give-on-approve.enabled", false);
    }

    public String getApproveRoleId() {
        return config.getString("roles.give-on-approve.role-id", "");
    }

    public List<String> getPlayerInfoAllowedRoleIds() {
        return config.getStringList("playerinfo.allowed-role-ids");
    }

    public List<String> getPlayerInfoAllowedUserIds() {
        return config.getStringList("playerinfo.allowed-user-ids");
    }

    public boolean isSuggestionsEnabled() {
        return config.getBoolean("suggestions.enabled", false);
    }

    public String getSuggestionsChannelId() {
        return config.getString("suggestions.channel-id", "");
    }

    public String getSuggestionsLogChannelId() {
        return config.getString("suggestions.log-channel-id", "");
    }

    public String getSuggestionUpvoteEmoji() {
        return config.getString("suggestions.voting.upvote-emoji", "👍");
    }

    public String getSuggestionDownvoteEmoji() {
        return config.getString("suggestions.voting.downvote-emoji", "👎");
    }

    public boolean isSuggestionPreventDoubleVote() {
        return config.getBoolean("suggestions.voting.prevent-double-vote", true);
    }

    public boolean isSuggestionReviewEnabled() {
        return config.getBoolean("suggestions.review.enabled", true);
    }

    public String getSuggestionApproveEmoji() {
        return config.getString("suggestions.review.approve-emoji", "🟢");
    }

    public String getSuggestionDenyEmoji() {
        return config.getString("suggestions.review.deny-emoji", "🔴");
    }

    public List<String> getSuggestionReviewAllowedRoleIds() {
        return config.getStringList("suggestions.review.allowed-role-ids");
    }

    public int getSuggestionPendingColor() {
        return parseColor("suggestions.embed.color-pending", "#F1C40F");
    }

    public int getSuggestionApprovedColor() {
        return parseColor("suggestions.embed.color-approved", "#2ECC71");
    }

    public int getSuggestionDeniedColor() {
        return parseColor("suggestions.embed.color-denied", "#E74C3C");
    }

    public String getSuggestionStatusPendingText() {
        return config.getString("suggestions.embed.status.pending", "In behandeling");
    }

    public String getSuggestionStatusApprovedText() {
        return config.getString("suggestions.embed.status.approved", "Goedgekeurd");
    }

    public String getSuggestionStatusDeniedText() {
        return config.getString("suggestions.embed.status.denied", "Afgekeurd");
    }

    public String getSuggestionEmbedTitle() {
        return config.getString("suggestions.embed.title", "Nieuwe suggestie");
    }

    public String getSuggestionLogEmbedTitle() {
        return config.getString("suggestions.embed.log-title", "Nieuwe suggestie • Staff review");
    }

    public String getSuggestionEmbedFooter() {
        return config.getString("suggestions.embed.footer", "Sakurahelm Suggesties");
    }

    public String getSuggestionFieldIdName() {
        return config.getString("suggestions.embed.fields.id", "ID");
    }

    public String getSuggestionFieldSubmitterName() {
        return config.getString("suggestions.embed.fields.submitter", "Ingestuurd door");
    }

    public String getSuggestionFieldStatusName() {
        return config.getString("suggestions.embed.fields.status", "Status");
    }

    public String getSuggestionFieldReviewerName() {
        return config.getString("suggestions.embed.fields.reviewer", "Beoordeeld door");
    }

    public int getSuggestionMinLength() {
        return config.getInt("suggestions.limits.min-length", 5);
    }

    public int getSuggestionMaxLength() {
        return config.getInt("suggestions.limits.max-length", 500);
    }

    public int getApplicationExpiryHours() {
        return config.getInt("expiry.application-hours", 24);
    }

    public int getRetryNameExpiryHours() {
        return config.getInt("expiry.retry-name-hours", 24);
    }

    private int parseColor(String path, String fallbackHex) {
        String value = config.getString(path, fallbackHex);
        try {
            return Color.decode(value).getRGB() & 0xFFFFFF;
        } catch (Exception ex) {
            return Color.decode(fallbackHex).getRGB() & 0xFFFFFF;
        }
    }

    public record ApplicationField(
            String key,
            String label,
            String style,
            String placeholder,
            boolean required,
            int minLength,
            int maxLength,
            int order
    ) {
    }
}