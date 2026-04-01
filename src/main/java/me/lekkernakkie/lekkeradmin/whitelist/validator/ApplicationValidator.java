package me.lekkernakkie.lekkeradmin.whitelist.validator;

import me.lekkernakkie.lekkeradmin.model.application.WhitelistApplication;

public class ApplicationValidator {

    public ValidationResult validate(WhitelistApplication application) {

        if (application == null) {
            return new ValidationResult(false, "Application null");
        }

        if (application.getMinecraftName() == null || application.getMinecraftName().isBlank()) {
            return new ValidationResult(false, "Minecraft naam ontbreekt");
        }

        if (application.getMinecraftName().length() < 3) {
            return new ValidationResult(false, "Naam te kort");
        }

        if (application.getMinecraftName().length() > 16) {
            return new ValidationResult(false, "Naam te lang");
        }

        return new ValidationResult(true, "OK");
    }

    public record ValidationResult(boolean valid, String message) {
    }
}