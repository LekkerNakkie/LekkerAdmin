package me.lekkernakkie.lekkeradmin.database.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;

import java.io.File;
import java.io.IOException;

public class SqliteDatabase {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;

    public SqliteDatabase(LekkerAdmin plugin, DCBotConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public HikariDataSource createDataSource() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }

        File databaseFile = new File(dataFolder, config.getSqliteFile());

        try {
            File parent = databaseFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Could not create SQLite parent folder.");
            }

            if (!databaseFile.exists() && !databaseFile.createNewFile()) {
                throw new IllegalStateException("Could not create SQLite database file.");
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not initialize SQLite database file.", ex);
        }

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("LekkerAdmin-SQLite");
        hikari.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        hikari.setMaximumPoolSize(1);
        hikari.setConnectionTimeout(10000);
        hikari.setIdleTimeout(600000);
        hikari.setMaxLifetime(1800000);

        hikari.addDataSourceProperty("foreign_keys", "true");

        plugin.getLogger().info("Preparing SQLite connection at " + databaseFile.getAbsolutePath());

        return new HikariDataSource(hikari);
    }
}