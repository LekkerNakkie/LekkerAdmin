package me.lekkernakkie.lekkeradmin.database;

import com.zaxxer.hikari.HikariDataSource;
import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;
import me.lekkernakkie.lekkeradmin.database.mysql.MySqlDatabase;
import me.lekkernakkie.lekkeradmin.database.sqlite.SqliteDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class DatabaseManager {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;

    private DatabaseType databaseType;
    private HikariDataSource dataSource;
    private ExecutorService databaseExecutor;

    public DatabaseManager(LekkerAdmin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getDcBotConfig();
    }

    public void connect() {
        this.databaseType = DatabaseType.fromString(config.getDatabaseType());

        switch (databaseType) {
            case MYSQL -> this.dataSource = new MySqlDatabase(plugin, config).createDataSource();
            case SQLITE -> this.dataSource = new SqliteDatabase(plugin, config).createDataSource();
        }

        if (this.dataSource == null) {
            throw new IllegalStateException("Failed to create database datasource.");
        }

        try (Connection connection = getConnection()) {
            if (connection == null || connection.isClosed()) {
                throw new IllegalStateException("Database connection test failed.");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to connect to database.", ex);
        }

        if (databaseExecutor == null || databaseExecutor.isShutdown()) {
            int poolSize = Math.max(2, config.getMysqlPoolSize());
            this.databaseExecutor = Executors.newFixedThreadPool(
                    poolSize,
                    runnable -> {
                        Thread thread = new Thread(runnable, "LekkerAdmin-DB");
                        thread.setDaemon(true);
                        return thread;
                    }
            );
        }
    }

    public void disconnect() {
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
            try {
                if (!databaseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    databaseExecutor.shutdownNow();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                databaseExecutor.shutdownNow();
            }
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Datasource is not initialized.");
        }
        return dataSource.getConnection();
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        if (databaseExecutor == null || databaseExecutor.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Database executor is not running."));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        }, databaseExecutor);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        if (databaseExecutor == null || databaseExecutor.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Database executor is not running."));
        }

        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        }, databaseExecutor);
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }
}