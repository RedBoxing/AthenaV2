package fr.redboxing.athena.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.redboxing.athena.BotConfig;

public class DatabaseManager {
    private static HikariDataSource hikariDataSource;

    public static HikariDataSource getHikariDataSource() {
        if(hikariDataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:" + BotConfig.get("DATABASE_DIALECT") + "://" + BotConfig.get("DATABASE_HOST") + ":" + BotConfig.get("DATABASE_PORT") + "/" + BotConfig.get("DATABASE_DB"));
            config.setUsername(BotConfig.get("DATABASE_USER"));
            config.setPassword(BotConfig.get("DATABASE_PASSWORD"));
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.setMaximumPoolSize(5000);
            hikariDataSource = new HikariDataSource(config);
        }

        return hikariDataSource;
    }
}
