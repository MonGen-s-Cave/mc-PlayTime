package com.mongenscave.mcplaytime.hooks.plugins;

import com.mongenscave.mcplaytime.McPlayTime;
import com.mongenscave.mcplaytime.identifiers.keys.ConfigKeys;
import com.mongenscave.mcplaytime.utils.LoggerUtils;
import com.mongenscave.mcplaytime.utils.TimeUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public class PlaceholderAPI {
    public static boolean isRegistered = false;

    public static void registerHook() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderIntegration().register();
            isRegistered = true;
        }
    }

    private static class PlaceholderIntegration extends PlaceholderExpansion {
        private static final McPlayTime plugin = McPlayTime.getInstance();
        private ConcurrentHashMap<String, Long> topPlayersCache = new ConcurrentHashMap<>();
        private List<String> topPlayerNames = Collections.synchronizedList(new ArrayList<>());
        private long lastCacheUpdate = 0;
        private static final long CACHE_DURATION = 60000;

        @Override
        public @NotNull String getIdentifier() {
            return "mcplaytime";
        }

        @Override
        public @NotNull String getAuthor() {
            return "coma112";
        }

        @Override
        public @NotNull String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public boolean canRegister() {
            return true;
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer player, @NotNull String params) {
            if (player == null) return "";

            String[] args = params.split("_");
            String type = args[0].toLowerCase();

            return switch (type) {
                case "current" -> getCurrentServerTime(player);
                case "total" -> getTotalTime(player);
                case "session" -> getSessionTime(player);
                case "server" -> getServerTime(player, args.length > 1 ? args[1] : ConfigKeys.SERVER.getString());
                case "milliseconds" -> getTimeInFormat(player, args, "milliseconds");
                case "seconds" -> getTimeInFormat(player, args, "seconds");
                case "minutes" -> getTimeInFormat(player, args, "minutes");
                case "hours" -> getTimeInFormat(player, args, "hours");
                case "days" -> getTimeInFormat(player, args, "days");
                case "top" -> getTopPlayerInfo(args);
                default -> "Invalid placeholder";
            };
        }

        private void updateTopPlayersCache(@NotNull String serverName) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastCacheUpdate > CACHE_DURATION) {
                try {
                    CompletableFuture<ConcurrentHashMap<String, Long>> future = McPlayTime.getInstance().getService().getTopPlayers(serverName, 10);
                    ConcurrentHashMap<String, Long> topData = future.get(2, TimeUnit.SECONDS);

                    topPlayerNames.clear();
                    topPlayersCache.clear();

                    topData.entrySet().stream()
                            .sorted(ConcurrentHashMap.Entry.<String, Long>comparingByKey().reversed())
                            .forEach(entry -> {
                                topPlayerNames.add(entry.getKey());
                                topPlayersCache.put(entry.getKey(), entry.getValue());
                            });

                    lastCacheUpdate = currentTime;
                } catch (Exception exception) {
                    LoggerUtils.error(exception.getMessage());
                }
            }
        }

        @NotNull
        private String getTopPlayerInfo(@NotNull String[] args) {
            if (args.length < 3) return "Invalid format";

            String infoType = args[1].toLowerCase();
            String positionString = args[2];
            String serverName = args.length > 3 ? args[3] : ConfigKeys.SERVER.getString();

            int position;

            try {
                position = Integer.parseInt(positionString);
                if (position < 1) return "Invalid position";
            } catch (NumberFormatException exception) {
                return "Invalid position";
            }

            updateTopPlayersCache(serverName);

            switch (infoType) {
                case "player" -> getTopPlayerName(position);
                case "value" -> getTopPlayerValue(position);
            }

            return infoType;
        }

        private String getTopPlayerName(int position) {
            if (position <= topPlayerNames.size()) return topPlayerNames.get(position - 1);
            return "N/A";
        }

        @NotNull
        private String getTopPlayerValue(int position) {
            if (position <= topPlayerNames.size()) {
                String playerName = topPlayerNames.get(position - 1);
                Long time = topPlayersCache.get(playerName);

                if (time != null) return TimeUtils.formatTime(time);
            }

            return "0s";
        }

        private String getCurrentServerTime(OfflinePlayer player) {
            try {
                CompletableFuture<String> future = plugin.getService().getPlayerTimeFormatted(player.getUniqueId(), ConfigKeys.SERVER.getString());
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception exception) {
                return "Loading...";
            }
        }

        private String getTotalTime(OfflinePlayer player) {
            try {
                CompletableFuture<String> future = plugin.getService().getTotalPlayerTimeFormatted(player.getUniqueId());
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception exception) {
                return "Loading...";
            }
        }

        private String getSessionTime(OfflinePlayer player) {
            try {
                CompletableFuture<String> future = plugin.getService().getCurrentSessionTimeFormatted(player.getUniqueId());
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception exception) {
                return "0s";
            }
        }

        private String getServerTime(OfflinePlayer player, String serverName) {
            try {
                CompletableFuture<String> future = plugin.getService()
                        .getPlayerTimeFormatted(player.getUniqueId(), serverName);
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception exception) {
                return "Loading...";
            }
        }

        private String getTimeInFormat(OfflinePlayer player, @NotNull String[] args, String format) {
            if (args.length < 2) return "0";

            String timeType = args[1].toLowerCase();

            try {
                CompletableFuture<Long> future = switch (timeType) {
                    case "current" -> plugin.getService().getPlayerTime(player.getUniqueId(), ConfigKeys.SERVER.getString());
                    case "total" -> plugin.getService().getTotalPlayerTime(player.getUniqueId());
                    case "session" -> plugin.getService().getCurrentSessionTime(player.getUniqueId());
                    case "server" -> {
                        String serverName = args.length > 2 ? args[2] : ConfigKeys.SERVER.getString();
                        yield plugin.getService().getPlayerTime(player.getUniqueId(), serverName);
                    }
                    default -> CompletableFuture.completedFuture(0L);
                };

                long milliseconds = future.get(1, TimeUnit.SECONDS);

                return String.valueOf(switch (format) {
                    case "milliseconds" -> milliseconds;
                    case "seconds" -> TimeUnit.MILLISECONDS.toSeconds(milliseconds);
                    case "minutes" -> TimeUnit.MILLISECONDS.toMinutes(milliseconds);
                    case "hours" -> TimeUnit.MILLISECONDS.toHours(milliseconds);
                    case "days" -> TimeUnit.MILLISECONDS.toDays(milliseconds);
                    default -> 0L;
                });

            } catch (Exception exception) {
                return "0";
            }
        }
    }
}