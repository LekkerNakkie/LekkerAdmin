package me.lekkernakkie.lekkeradmin.service.whitelist;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsernameValidationService {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;
    private final Pattern minecraftNamePattern;

    public UsernameValidationService(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
        this.minecraftNamePattern = Pattern.compile(config.getMinecraftNameRegex());
    }

    public ValidationResult validate(String minecraftName) {
        if (minecraftName == null || minecraftName.isBlank()) {
            return new ValidationResult(false, "Minecraft naam ontbreekt.", null);
        }

        String input = minecraftName.trim();

        if (!config.isMinecraftNameValidationEnabled()) {
            return new ValidationResult(true, "Validatie uitgeschakeld.", null);
        }

        if (input.length() < config.getMinecraftNameMinLength()) {
            return new ValidationResult(false, "Minecraft naam is te kort.", null);
        }

        if (input.length() > config.getMinecraftNameMaxLength()) {
            return new ValidationResult(false, "Minecraft naam is te lang.", null);
        }

        if (!minecraftNamePattern.matcher(input).matches()) {
            return new ValidationResult(false, "Minecraft naam bevat ongeldige tekens.", null);
        }

        if (!config.isCheckProfileExistsEnabled()) {
            return new ValidationResult(true, "Naam syntactisch geldig.", null);
        }

        try {
            LookupResult lookup = fetchUuid(input);

            return switch (lookup.status()) {
                case FOUND -> new ValidationResult(true, "Minecraft profiel gevonden.", lookup.uuid());
                case NOT_FOUND -> new ValidationResult(false, "Minecraft profiel bestaat niet.", null);
                case TEMP_ERROR -> new ValidationResult(
                        false,
                        "Minecraft profiel kon tijdelijk niet geverifieerd worden. Probeer later opnieuw.",
                        null
                );
            };
        } catch (Exception ex) {
            plugin.getLogger().warning("Minecraft naam-validatie crashte voor '" + input + "': " + ex.getMessage());
            return new ValidationResult(
                    false,
                    "Minecraft profiel kon tijdelijk niet geverifieerd worden. Probeer later opnieuw.",
                    null
            );
        }
    }

    private LookupResult fetchUuid(String username) throws Exception {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + username;

        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "LekkerAdmin/2.0");

        int responseCode = connection.getResponseCode();
        String body = readBody(connection);

        plugin.debug("[UsernameValidation] Lookup username='" + username
                + "' responseCode=" + responseCode
                + " body=" + (body == null || body.isBlank() ? "<empty>" : body));

        if (responseCode == 204 || responseCode == 404) {
            connection.disconnect();
            return new LookupResult(LookupStatus.NOT_FOUND, null);
        }

        if (responseCode == 429 || responseCode == 403 || responseCode >= 500) {
            connection.disconnect();
            return new LookupResult(LookupStatus.TEMP_ERROR, null);
        }

        if (responseCode < 200 || responseCode >= 300) {
            connection.disconnect();
            return new LookupResult(LookupStatus.TEMP_ERROR, null);
        }

        if (body == null || body.isBlank()) {
            connection.disconnect();
            return new LookupResult(LookupStatus.TEMP_ERROR, null);
        }

        String id = extractJsonValue(body, "id");

        if (id == null || id.length() != 32) {
            connection.disconnect();
            return new LookupResult(LookupStatus.TEMP_ERROR, null);
        }

        String formattedUuid = id.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );

        connection.disconnect();
        return new LookupResult(LookupStatus.FOUND, formattedUuid);
    }

    private String readBody(HttpURLConnection connection) {
        try {
            InputStream stream = connection.getResponseCode() >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();

            if (stream == null) {
                return "";
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            )) {
                StringBuilder json = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                return json.toString();
            }
        } catch (Exception ex) {
            return "";
        }
    }

    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private enum LookupStatus {
        FOUND,
        NOT_FOUND,
        TEMP_ERROR
    }

    private record LookupResult(LookupStatus status, String uuid) {
    }

    public record ValidationResult(boolean valid, String reason, String minecraftUuid) {
    }
}