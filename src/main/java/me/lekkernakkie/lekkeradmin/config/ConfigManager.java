package me.lekkernakkie.lekkeradmin.config;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.logs.LogsConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final LekkerAdmin plugin;

    private MainConfig mainConfig;
    private DCBotConfig dcBotConfig;
    private MessagesConfig messagesConfig;
    private PunishmentsConfig punishmentsConfig;
    private LogsConfig logsConfig;

    private FileConfiguration dcBotYaml;
    private FileConfiguration punishmentsYaml;
    private FileConfiguration logsYaml;

    public ConfigManager(LekkerAdmin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.reloadConfig();
        loadDcBotYaml();
        loadPunishmentsYaml();
        loadLogsYaml();

        this.mainConfig = new MainConfig(plugin.getConfig());
        this.dcBotConfig = new DCBotConfig(dcBotYaml);
        this.messagesConfig = new MessagesConfig(dcBotYaml);
        this.punishmentsConfig = new PunishmentsConfig(punishmentsYaml);
        this.logsConfig = new LogsConfig(logsYaml);
    }

    public void reloadAll() {
        loadAll();
    }

    private void loadDcBotYaml() {
        File file = new File(plugin.getDataFolder(), "DCBot.yml");
        this.dcBotYaml = YamlConfiguration.loadConfiguration(file);
    }

    private void loadPunishmentsYaml() {
        File file = new File(plugin.getDataFolder(), "Punishments.yml");
        this.punishmentsYaml = YamlConfiguration.loadConfiguration(file);
    }

    private void loadLogsYaml() {
        File file = new File(plugin.getDataFolder(), "logs.yml");
        this.logsYaml = YamlConfiguration.loadConfiguration(file);
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public DCBotConfig getDcBotConfig() {
        return dcBotConfig;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public PunishmentsConfig getPunishmentsConfig() {
        return punishmentsConfig;
    }

    public LogsConfig getLogsConfig() {
        return logsConfig;
    }

    public FileConfiguration getDcBotYaml() {
        return dcBotYaml;
    }

    public FileConfiguration getPunishmentsYaml() {
        return punishmentsYaml;
    }

    public FileConfiguration getLogsYaml() {
        return logsYaml;
    }
}