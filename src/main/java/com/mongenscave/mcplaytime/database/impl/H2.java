package com.mongenscave.mcplaytime.database.impl;

import com.mongenscave.mcplaytime.McPlayTime;
import com.mongenscave.mcplaytime.config.Config;
import com.mongenscave.mcplaytime.database.Database;
import com.mongenscave.mcplaytime.model.PlayerData;
import com.mongenscave.mcplaytime.utils.LoggerUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class H2 implements Database {
    private static final McPlayTime plugin = McPlayTime.getInstance();
    private HikariDataSource dataSource;

    @Override
    public void initialize() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String dbPath = new File(dataFolder, "mcplaytime").getAbsolutePath();
        hikariConfig.setJdbcUrl("jdbc:h2:" + dbPath + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        hikariConfig.setDriverClassName("org.h2.Driver");

        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setLeakDetectionThreshold(60000);

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(hikariConfig);

        createTables();
    }

    private void createTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS onlinetime_players (
                uuid VARCHAR(36) NOT NULL,
                username VARCHAR(16) NOT NULL,
                server VARCHAR(32) NOT NULL,
                total_time BIGINT NOT NULL DEFAULT 0,
                session_start BIGINT DEFAULT NULL,
                last_seen BIGINT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (uuid, server)
            );
            
            CREATE INDEX IF NOT EXISTS idx_username ON onlinetime_players(username);
            CREATE INDEX IF NOT EXISTS idx_server ON onlinetime_players(server);
            CREATE INDEX IF NOT EXISTS idx_total_time ON onlinetime_players(total_time);
            CREATE INDEX IF NOT EXISTS idx_last_seen ON onlinetime_players(last_seen);
            """;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public CompletableFuture<PlayerData> getPlayerData(UUID uuid, String server) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM onlinetime_players WHERE uuid = ? AND server = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, server);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return PlayerData.builder()
                                .uuid(UUID.fromString(rs.getString("uuid")))
                                .username(rs.getString("username"))
                                .server(rs.getString("server"))
                                .totalTime(rs.getLong("total_time"))
                                .sessionStart(rs.getLong("session_start"))
                                .lastSeen(rs.getLong("last_seen"))
                                .build();
                    }
                }
            } catch (SQLException exception) {
                LoggerUtils.error(exception.getMessage());
            }

            return null;
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                MERGE INTO onlinetime_players (uuid, username, server, total_time, session_start, last_seen, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, data.getUuid().toString());
                stmt.setString(2, data.getUsername());
                stmt.setString(3, data.getServer());
                stmt.setLong(4, data.getTotalTime());
                stmt.setObject(5, data.getSessionStart() == 0 ? null : data.getSessionStart());
                stmt.setLong(6, data.getLastSeen());

                stmt.executeUpdate();

            } catch (SQLException exception) {
                LoggerUtils.error(exception.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<ConcurrentHashMap<String, Long>> getTopPlayers(String server, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<String, Long> topPlayers = new ConcurrentHashMap<>();
            String sql = "SELECT username, total_time FROM onlinetime_players WHERE server = ? ORDER BY total_time DESC LIMIT ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, server);
                stmt.setInt(2, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        topPlayers.put(rs.getString("username"), rs.getLong("total_time"));
                    }
                }
            } catch (SQLException exception) {
                LoggerUtils.error(exception.getMessage());
            }

            return topPlayers;
        });
    }

    @Override
    public CompletableFuture<Long> getTotalOnlineTime(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT SUM(total_time) as total FROM onlinetime_players WHERE uuid = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getLong("total");
                }
            } catch (SQLException exception) {
                LoggerUtils.error(exception.getMessage());
            }

            return 0L;
        });
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}