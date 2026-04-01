package me.lekkernakkie.lekkeradmin.service.application;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;

public class NameRetryService {

    private final DCBotConfig config;

    public NameRetryService(LekkerAdmin plugin) {
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public RetryResult processRetry(WhitelistApplication application, String newMinecraftName) {
        if (application == null) {
            return new RetryResult(false, "Applicatie is null.", null);
        }

        if (newMinecraftName == null || newMinecraftName.isBlank()) {
            return new RetryResult(false, "Minecraft naam is leeg.", null);
        }

        String trimmed = newMinecraftName.trim();

        if (trimmed.length() < config.getMinecraftNameMinLength() || trimmed.length() > config.getMinecraftNameMaxLength()) {
            return new RetryResult(
                    false,
                    "Minecraft naam moet tussen " + config.getMinecraftNameMinLength() + " en " + config.getMinecraftNameMaxLength() + " tekens zijn.",
                    null
            );
        }

        if (!trimmed.matches(config.getMinecraftNameRegex())) {
            return new RetryResult(false, "Minecraft naam bevat ongeldige tekens.", null);
        }

        application.setMinecraftName(trimmed);
        return new RetryResult(true, "Naamcorrectie verwerkt.", null);
    }

    public record RetryResult(boolean success, String message, String minecraftUuid) {
    }
}