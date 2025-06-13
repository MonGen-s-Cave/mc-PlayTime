package com.mongenscave.mcplaytime.database;

import com.mongenscave.mcplaytime.McPlayTime;
import com.mongenscave.mcplaytime.config.Config;
import com.mongenscave.mcplaytime.model.PlayerData;
import com.mongenscave.mcplaytime.utils.LoggerUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class MySQL {
    private static final McPlayTime plugin = McPlayTime.getInstance();
    private static final Config config = plugin.getConfiguration();
    private HikariDataSource dataSource;

    public void initialize() throws SQLException {
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "badge");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "");
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        this.dataSource = new HikariDataSource(config);

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
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (uuid, server),
                INDEX idx_username (username),
                INDEX idx_server (server),
                INDEX idx_total_time (total_time),
                INDEX idx_last_seen (last_seen)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

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

    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO onlinetime_players (uuid, username, server, total_time, session_start, last_seen)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                total_time = VALUES(total_time),
                session_start = VALUES(session_start),
                last_seen = VALUES(last_seen)
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

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
