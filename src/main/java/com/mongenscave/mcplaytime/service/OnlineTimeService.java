package com.mongenscave.mcplaytime.service;

import com.mongenscave.mcplaytime.McPlayTime;
import com.mongenscave.mcplaytime.identifiers.keys.ConfigKeys;
import com.mongenscave.mcplaytime.model.PlayerData;
import com.mongenscave.mcplaytime.utils.TimeUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineTimeService {
    private static final McPlayTime plugin = McPlayTime.getInstance();
    private final ConcurrentHashMap<UUID, PlayerData> onlinePlayerCache = new ConcurrentHashMap<>();

    public CompletableFuture<Void> handlePlayerJoin(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        String serverName = ConfigKeys.SERVER.getString();

        return plugin.getDatabase().getPlayerData(uuid, serverName)
                .thenAccept(data -> {
                    if (data == null) {
                        data = PlayerData.builder()
                                .uuid(uuid)
                                .username(player.getName())
                                .server(serverName)
                                .totalTime(0L)
                                .sessionStart(System.currentTimeMillis())
                                .lastSeen(System.currentTimeMillis())
                                .build();
                    } else {
                        data.setUsername(player.getName());
                        data.setSessionStart(System.currentTimeMillis());
                        data.setLastSeen(System.currentTimeMillis());
                    }

                    onlinePlayerCache.put(uuid, data);

                    plugin.getDatabase().savePlayerData(data);
                });
    }

    public CompletableFuture<Void> handlePlayerQuit(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = onlinePlayerCache.remove(uuid);

        if (data != null) {
            long sessionTime = System.currentTimeMillis() - data.getSessionStart();
            data.setTotalTime(data.getTotalTime() + sessionTime);
            data.setSessionStart(0);
            data.setLastSeen(System.currentTimeMillis());

            return plugin.getDatabase().savePlayerData(data);
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<String> getPlayerTimeFormatted(@NotNull UUID uuid, @NotNull String server) {
        PlayerData cachedData = onlinePlayerCache.get(uuid);

        if (cachedData != null && cachedData.getServer().equals(server)) {
            long totalTime = cachedData.getTotalTimeWithCurrentSession();
            return CompletableFuture.completedFuture(TimeUtils.formatTime(totalTime));
        }

        return plugin.getDatabase().getPlayerData(uuid, server)
                .thenApply(data -> {
                    if (data == null) return "0s";
                    return TimeUtils.formatTime(data.getTotalTime());
                });
    }

    public CompletableFuture<String> getTotalPlayerTimeFormatted(UUID uuid) {
        return plugin.getDatabase().getTotalOnlineTime(uuid)
                .thenApply(totalTime -> {
                    PlayerData cachedData = onlinePlayerCache.get(uuid);
                    if (cachedData != null) totalTime += cachedData.getCurrentSessionTime();
                    return TimeUtils.formatTime(totalTime);
                });
    }

    public CompletableFuture<Long> getPlayerTime(UUID uuid, String server) {
        PlayerData cachedData = onlinePlayerCache.get(uuid);

        if (cachedData != null && cachedData.getServer().equals(server)) return CompletableFuture.completedFuture(cachedData.getTotalTimeWithCurrentSession());

        return plugin.getDatabase().getPlayerData(uuid, server)
                .thenApply(data -> data == null ? 0L : data.getTotalTime());
    }

    public CompletableFuture<Long> getTotalPlayerTime(UUID uuid) {
        return plugin.getDatabase().getTotalOnlineTime(uuid)
                .thenApply(totalTime -> {
                    PlayerData cachedData = onlinePlayerCache.get(uuid);
                    if (cachedData != null) totalTime += cachedData.getCurrentSessionTime();
                    return totalTime;
                });
    }

    public CompletableFuture<String> getCurrentSessionTimeFormatted(UUID uuid) {
        PlayerData cachedData = onlinePlayerCache.get(uuid);

        if (cachedData != null) return CompletableFuture.completedFuture(TimeUtils.formatTime(cachedData.getCurrentSessionTime()));
        return CompletableFuture.completedFuture("0s");
    }

    public CompletableFuture<Long> getCurrentSessionTime(UUID uuid) {
        PlayerData cachedData = onlinePlayerCache.get(uuid);
        if (cachedData != null) return CompletableFuture.completedFuture(cachedData.getCurrentSessionTime());
        return CompletableFuture.completedFuture(0L);
    }

    public CompletableFuture<ConcurrentHashMap<String, Long>> getTopPlayers(String server, int limit) {
        return plugin.getDatabase().getTopPlayers(server, limit);
    }

    public void saveAllOnlinePlayers() {
        for (PlayerData data : onlinePlayerCache.values()) {
            data.setLastSeen(System.currentTimeMillis());
            plugin.getDatabase().savePlayerData(data);
        }
    }

    public boolean isPlayerOnline(UUID uuid) {
        return onlinePlayerCache.containsKey(uuid);
    }

    public PlayerData getCachedPlayerData(UUID uuid) {
        return onlinePlayerCache.get(uuid);
    }
}
