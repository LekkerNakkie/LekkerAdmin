package me.lekkernakkie.lekkeradmin;

import me.lekkernakkie.lekkeradmin.command.*;
import me.lekkernakkie.lekkeradmin.config.ConfigManager;
import me.lekkernakkie.lekkeradmin.config.LangConfig;
import me.lekkernakkie.lekkeradmin.database.DatabaseManager;
import me.lekkernakkie.lekkeradmin.database.migration.MigrationRunner;
import me.lekkernakkie.lekkeradmin.discord.*;
import me.lekkernakkie.lekkeradmin.discord.log.*;
import me.lekkernakkie.lekkeradmin.hook.*;
import me.lekkernakkie.lekkeradmin.listener.MaintenanceLoginListener;
import me.lekkernakkie.lekkeradmin.listener.VanishListener;
import me.lekkernakkie.lekkeradmin.listener.freeze.FreezeJoinQuitListener;
import me.lekkernakkie.lekkeradmin.listener.freeze.FreezeListener;
import me.lekkernakkie.lekkeradmin.listener.inventory.PendingChangesPlayerListener;
import me.lekkernakkie.lekkeradmin.listener.invsee.InvseeClickListener;
import me.lekkernakkie.lekkeradmin.listener.invsee.InvseeCloseListener;
import me.lekkernakkie.lekkeradmin.listener.invsee.InvseeDragListener;
import me.lekkernakkie.lekkeradmin.listener.log.*;
import me.lekkernakkie.lekkeradmin.punishment.command.*;
import me.lekkernakkie.lekkeradmin.punishment.listener.*;
import me.lekkernakkie.lekkeradmin.punishment.service.*;
import me.lekkernakkie.lekkeradmin.service.MaintenanceService;
import me.lekkernakkie.lekkeradmin.service.RestartService;
import me.lekkernakkie.lekkeradmin.service.freeze.FreezeService;
import me.lekkernakkie.lekkeradmin.service.invsee.InvseeService;
import me.lekkernakkie.lekkeradmin.service.log.ExplosionTrackerService;
import me.lekkernakkie.lekkeradmin.service.vanish.VanishService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class LekkerAdmin extends JavaPlugin {

    private static LekkerAdmin instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private MigrationRunner migrationRunner;
    private DiscordManager discordManager;

    private PunishmentService punishmentService;
    private PunishmentExpiryService punishmentExpiryService;
    private WarnService warnService;
    private DiscordPunishmentLogger discordPunishmentLogger;
    private ItemLogAggregationService itemLogAggregationService;
    private MinecraftLogDispatcher minecraftLogDispatcher;
    private InvseeService invseeService;
    private RestartService restartService;
    private MaintenanceService maintenanceService;
    private ExplosionTrackerService explosionTrackerService;
    private VanishService vanishService;
    private FreezeService freezeService;

    @Override
    public void onEnable() {
        instance = this;

        createDefaultFiles();

        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        this.migrationRunner = new MigrationRunner(this, databaseManager);
        this.migrationRunner.runMigrations();

        createRuntimeServices();

        registerHooks();
        registerCommands();
        registerListeners();
        startRuntimeServices();

        printStartupBanner();
    }

    @Override
    public void onDisable() {
        shutdownRuntimeServices();

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        printShutdownBanner();
    }

    public void reloadPlugin() {
        long start = System.currentTimeMillis();

        console("&b&m-------------------&9 LekkerAdmin Reload &b&m-------------------");
        console(lang().format("reload.started", "&7Reload gestart...", null));

        shutdownRuntimeServices();

        HandlerList.unregisterAll(this);

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        reloadConfig();
        configManager.reloadAll();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        this.migrationRunner = new MigrationRunner(this, databaseManager);
        this.migrationRunner.runMigrations();

        createRuntimeServices();
        registerCommands();
        registerListeners();
        startRuntimeServices();

        long time = System.currentTimeMillis() - start;

        console(lang().format("reload.config", "&a✔ &7Config herladen", null));
        console(lang().format("reload.database", "&a✔ &7Database reconnect uitgevoerd", null));
        console(lang().format("reload.discord", "&a✔ &7Discord bot herstart", null));
        console(lang().format("reload.punishments", "&a✔ &7Punishments herladen", null));
        console(lang().format("reload.punishment-cache", "&a✔ &7Punishment cache herladen", null));
        console(lang().format("reload.expirations", "&a✔ &7Expirations opnieuw ingepland", null));
        console(lang().format("reload.logs", "&a✔ &7Logs herladen", null));
        console(lang().format("reload.invsee", "&a✔ &7Invsee herladen", null));
        console(lang().format("reload.restart", "&a✔ &7Restart systeem herladen", null));
        console(lang().format("reload.maintenance", "&a✔ &7Maintenance systeem herladen", null));
        console(lang().format("reload.explosions", "&a✔ &7Explosion logs herladen", null));
        console(lang().format("reload.sessions", "&a✔ &7Offline sessies herladen", null));
        console(lang().format("reload.vanish", "&a✔ &7Vanish herladen", null));
        console(lang().format("reload.finished", "&7Reload voltooid in &b{time}ms&7.", Map.of("time", String.valueOf(time))));
        console("&b&m--------------------------------------------------------------");
    }

    private void createRuntimeServices() {
        this.discordPunishmentLogger = new DiscordPunishmentLogger(this);
        this.punishmentService = new PunishmentService(this);
        this.punishmentExpiryService = new PunishmentExpiryService(this);
        this.warnService = new WarnService(this);
        this.itemLogAggregationService = new ItemLogAggregationService(this);
        this.minecraftLogDispatcher = new MinecraftLogDispatcher(this);
        this.invseeService = new InvseeService(this);
        this.restartService = new RestartService(this);
        this.maintenanceService = new MaintenanceService(this);
        this.explosionTrackerService = new ExplosionTrackerService(this);
        this.vanishService = new VanishService(this);
        this.freezeService = new FreezeService(this);
    }

    private void startRuntimeServices() {
        startDiscord();

        punishmentService.loadCacheAndReschedule();
        punishmentExpiryService.start();
        restartService.start();
    }

    private void shutdownRuntimeServices() {
        if (restartService != null) {
            restartService.shutdown();
        }

        if (punishmentExpiryService != null) {
            punishmentExpiryService.stop();
        }

        if (punishmentService != null) {
            punishmentService.cancelAllScheduledExpirations();
        }

        if (vanishService != null) {
            vanishService.shutdown();
        }

        if (freezeService != null) {
            freezeService.shutdown();
        }

        if (discordManager != null) {
            discordManager.shutdown();
            discordManager = null;
        }
    }

    private void registerHooks() {
        new PlaceholderApiHook(this).hook();
        new LuckPermsHook(this).hook();
    }

    private void registerCommands() {
        PluginCommand lekkerAdminCommand = getCommand("lekkeradmin");
        if (lekkerAdminCommand == null) {
            getLogger().warning("Command 'lekkeradmin' not found in plugin.yml");
        } else {
            lekkerAdminCommand.setExecutor(new LekkerAdminCommand(this));
            lekkerAdminCommand.setTabCompleter(new LekkerAdminCommandTabCompleter());
        }

        PluginCommand invseeAlias = getCommand("invsee");
        if (invseeAlias == null) {
            getLogger().warning("Command 'invsee' not found in plugin.yml");
        } else {
            invseeAlias.setExecutor(new InvseeAliasCommand());
        }

        PluginCommand enderchestAlias = getCommand("enderchest");
        if (enderchestAlias == null) {
            getLogger().warning("Command 'enderchest' not found in plugin.yml");
        } else {
            enderchestAlias.setExecutor(new EnderChestAliasCommand());
        }

        PluginCommand maintenanceAlias = getCommand("maintenance");
        if (maintenanceAlias == null) {
            getLogger().warning("Command 'maintenance' not found in plugin.yml");
        } else {
            maintenanceAlias.setExecutor(new MaintenanceAliasCommand(this));
        }

        RestartAliasCommand restartAliasCommand = new RestartAliasCommand(this);

        PluginCommand planRestartAlias = getCommand("planrestart");
        if (planRestartAlias == null) {
            getLogger().warning("Command 'planrestart' not found in plugin.yml");
        } else {
            planRestartAlias.setExecutor(restartAliasCommand);
            planRestartAlias.setTabCompleter(restartAliasCommand);
        }

        PluginCommand cancelRestartAlias = getCommand("cancelrestart");
        if (cancelRestartAlias == null) {
            getLogger().warning("Command 'cancelrestart' not found in plugin.yml");
        } else {
            cancelRestartAlias.setExecutor(restartAliasCommand);
            cancelRestartAlias.setTabCompleter(restartAliasCommand);
        }

        WhoisCommand whoisCommand = new WhoisCommand(this);
        PluginCommand whois = getCommand("whois");
        if (whois == null) {
            getLogger().warning("Command 'whois' not found in plugin.yml");
        } else {
            whois.setExecutor(whoisCommand);
            whois.setTabCompleter(whoisCommand);
        }

        PluginCommand vanish = getCommand("vanish");
        if (vanish == null) {
            getLogger().warning("Command 'vanish' not found in plugin.yml");
        } else {
            vanish.setExecutor(new VanishCommand(this));
        }

        PluginCommand freeze = getCommand("freeze");
        if (freeze == null) {
            getLogger().warning("Command 'freeze' not found in plugin.yml");
        } else {
            freeze.setExecutor(new FreezeCommand(this));
        }

        PluginCommand unfreeze = getCommand("unfreeze");
        if (unfreeze == null) {
            getLogger().warning("Command 'unfreeze' not found in plugin.yml");
        } else {
            unfreeze.setExecutor(new UnfreezeCommand(this));
        }

        PluginCommand nextRestart = getCommand("nextrestart");
        if (nextRestart == null) {
            getLogger().warning("Command 'nextrestart' not found in plugin.yml");
        } else {
            nextRestart.setExecutor(new NextRestartCommand(this));
        }

        BanCommand banCommand = new BanCommand(this);
        UnbanCommand unbanCommand = new UnbanCommand(this);
        MuteCommand muteCommand = new MuteCommand(this);
        UnmuteCommand unmuteCommand = new UnmuteCommand(this);
        KickCommand kickCommand = new KickCommand(this);
        BanlistCommand banlistCommand = new BanlistCommand(this);
        WarnCommand warnCommand = new WarnCommand(this);
        HistoryCommand historyCommand = new HistoryCommand(this);
        ClearHistoryCommand clearHistoryCommand = new ClearHistoryCommand(this);

        registerPunishmentCommand("ban", banCommand, banCommand);
        registerPunishmentCommand("unban", unbanCommand, unbanCommand);
        registerPunishmentCommand("mute", muteCommand, muteCommand);
        registerPunishmentCommand("unmute", unmuteCommand, unmuteCommand);
        registerPunishmentCommand("kick", kickCommand, kickCommand);
        registerPunishmentCommand("banlist", banlistCommand, banlistCommand);
        registerPunishmentCommand("warn", warnCommand, warnCommand);
        registerPunishmentCommand("history", historyCommand, historyCommand);
        registerPunishmentCommand("clearhistory", clearHistoryCommand, clearHistoryCommand);
    }

    private void registerPunishmentCommand(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().warning("Command '" + commandName + "' not found in plugin.yml");
            return;
        }

        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PunishmentChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PunishmentLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new PunishmentNotifyJoinListener(this), this);

        getServer().getPluginManager().registerEvents(new PlayerDropLogListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerPickupLogListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathLogListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinLogListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerLeaveLogListener(this), this);
        getServer().getPluginManager().registerEvents(new TrackedItemDestroyListener(this), this);
        getServer().getPluginManager().registerEvents(new TrackedItemSpawnLinkListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosionLogListener(this), this);

        getServer().getPluginManager().registerEvents(new InvseeClickListener(this, invseeService), this);
        getServer().getPluginManager().registerEvents(new InvseeDragListener(this, invseeService), this);
        getServer().getPluginManager().registerEvents(new InvseeCloseListener(this, invseeService), this);
        getServer().getPluginManager().registerEvents(new PendingChangesPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MaintenanceLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeJoinQuitListener(this), this);
    }

    private void startDiscord() {
        this.discordManager = new DiscordBootstrap(this).start();
    }

    private void createDefaultFiles() {
        saveDefaultConfig();

        File dcBotFile = new File(getDataFolder(), "DCBot.yml");
        if (!dcBotFile.exists()) {
            saveResource("DCBot.yml", false);
        }

        File punishmentsFile = new File(getDataFolder(), "Punishments.yml");
        if (!punishmentsFile.exists()) {
            saveResource("Punishments.yml", false);
        }

        File logsFile = new File(getDataFolder(), "logs.yml");
        if (!logsFile.exists()) {
            saveResource("logs.yml", false);
        }

        File langFile = new File(getDataFolder(), "lang_nl.yml");
        if (!langFile.exists()) {
            saveResource("lang_nl.yml", false);
        }
    }

    private void printStartupBanner() {
        console("&b&m-------------------&9 LekkerAdmin &b&m-------------------");
        console("&bStatus&7: &aEnabled");
        console("&bAuthor&7: &9LekkerNakkie");
        console("&bDatabase&7: &f" + (databaseManager == null || databaseManager.getDatabaseType() == null
                ? "Unknown"
                : databaseManager.getDatabaseType().name()));
        console("&bDiscord&7: &f" + (configManager.getDcBotConfig().isBotEnabled() ? "Enabled" : "Disabled"));
        console("&bPunishments&7: &f" + (configManager.getPunishmentsConfig().isEnabled() ? "Enabled" : "Disabled"));
        console("&bLogs&7: &f" + (configManager.getLogsConfig() != null && configManager.getLogsConfig().isEnabled() ? "Enabled" : "Disabled"));
        console("&bInvsee&7: &f" + (configManager.getMainConfig().isInvseeEnabled() ? "Enabled" : "Disabled"));
        console("&bEnderchest&7: &f" + (configManager.getMainConfig().isEnderchestEnabled() ? "Enabled" : "Disabled"));
        console("&bWhois&7: &f" + (configManager.getMainConfig().isWhoisEnabled() ? "Enabled" : "Disabled"));
        console("&bVanish&7: &f" + (configManager.getMainConfig().isVanishEnabled() ? "Enabled" : "Disabled"));
        console("&bFreeze&7: &f" + (getConfig().getBoolean("freeze.enabled", true) ? "Enabled" : "Disabled"));
        console("&bRestart&7: &f" + (configManager.getMainConfig().isRestartEnabled() ? "Enabled" : "Disabled"));
        console("&bMaintenance&7: &f" + (configManager.getMainConfig().isMaintenanceEnabled() ? "Enabled" : "Disabled"));
        console("&b&m--------------------------------------------------------");
    }

    private void printShutdownBanner() {
        console("&b&m-------------------&9 LekkerAdmin &b&m-------------------");
        console("&bStatus&7: &cDisabled");
        console("&bAuthor&7: &9LekkerNakkie");
        console("&b&m--------------------------------------------------------");
    }

    public void debug(String message) {
        if (configManager != null && configManager.getMainConfig().isDebugEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    private void console(String message) {
        if (message == null) {
            return;
        }

        String normalized = message.replace('§', '&');
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(normalized);
        Bukkit.getConsoleSender().sendMessage(component);
    }

    public LangConfig lang() {
        return configManager.getLangConfig();
    }

    public static LekkerAdmin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MigrationRunner getMigrationRunner() {
        return migrationRunner;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public PunishmentService getPunishmentService() {
        return punishmentService;
    }

    public PunishmentExpiryService getPunishmentExpiryService() {
        return punishmentExpiryService;
    }

    public WarnService getWarnService() {
        return warnService;
    }

    public DiscordPunishmentLogger getDiscordPunishmentLogger() {
        return discordPunishmentLogger;
    }

    public ItemLogAggregationService getItemLogAggregationService() {
        return itemLogAggregationService;
    }

    public MinecraftLogDispatcher getMinecraftLogDispatcher() {
        return minecraftLogDispatcher;
    }

    public InvseeService getInvseeService() {
        return invseeService;
    }

    public RestartService getRestartService() {
        return restartService;
    }

    public MaintenanceService getMaintenanceService() {
        return maintenanceService;
    }

    public ExplosionTrackerService getExplosionTrackerService() {
        return explosionTrackerService;
    }

    public VanishService getVanishService() {
        return vanishService;
    }

    public FreezeService getFreezeService() {
        return freezeService;
    }
}