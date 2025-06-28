package com.mongenscave.mcplaytime.database;

import com.mongenscave.mcplaytime.model.PlayerData;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public interface Database {
    void initialize() throws SQLException;
    Connection getConnection() throws SQLException;
    CompletableFuture<PlayerData> getPlayerData(UUID uuid, String server);
    CompletableFuture<Void> savePlayerData(PlayerData data);
    CompletableFuture<ConcurrentHashMap<String, Long>> getTopPlayers(String server, int limit);
    CompletableFuture<Long> getTotalOnlineTime(UUID uuid);
    void close();
}