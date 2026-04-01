package me.lekkernakkie.lekkeradmin.database.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.config.DCBotConfig;

public class MySqlDatabase {

    private final LekkerAdmin plugin;
    private final DCBotConfig config;

    public MySqlDatabase(LekkerAdmin plugin, DCBotConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public HikariDataSource createDataSource() {
        HikariConfig hikari = new HikariConfig();

        String jdbcUrl = "jdbc:mysql://"
                + config.getMysqlHost() + ":"
                + config.getMysqlPort() + "/"
                + config.getMysqlDatabase()
                + "?useSSL=" + config.isMysqlSsl()
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC";

        hikari.setPoolName("LekkerAdmin-MySQL");
        hikari.setJdbcUrl(jdbcUrl);
        hikari.setUsername(config.getMysqlUsername());
        hikari.setPassword(config.getMysqlPassword());
        hikari.setMaximumPoolSize(config.getMysqlPoolSize());
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(10000);
        hikari.setIdleTimeout(600000);
        hikari.setMaxLifetime(1800000);
        hikari.setKeepaliveTime(300000);

        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");

        plugin.getLogger().info("Preparing MySQL connection to " + config.getMysqlHost() + ":" + config.getMysqlPort());

        return new HikariDataSource(hikari);
    }
}