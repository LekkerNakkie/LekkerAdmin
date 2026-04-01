package me.lekkernakkie.lekkeradmin.whitelist.validator;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.service.whitelist.UsernameValidationService;

public class MinecraftNameValidator {

    private final UsernameValidationService usernameValidationService;

    public MinecraftNameValidator(LekkerAdmin plugin) {
        this.usernameValidationService = new UsernameValidationService(plugin);
    }

    public ValidationResult validate(String minecraftName) {
        UsernameValidationService.ValidationResult result = usernameValidationService.validate(minecraftName);

        if (!result.valid()) {
            return ValidationResult.invalid(result.reason());
        }

        return ValidationResult.valid(result.minecraftUuid());
    }

    public record ValidationResult(boolean valid, String message, String minecraftUuid) {
        public static ValidationResult valid(String minecraftUuid) {
            return new ValidationResult(true, "OK", minecraftUuid);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message, null);
        }
    }
}