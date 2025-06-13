package com.mongenscave.mcplaytime.hooks.plugins;

import com.mongenscave.mcplaytime.McPlayTime;
import com.mongenscave.mcplaytime.identifiers.keys.ConfigKeys;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
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

        @Override
        public @NotNull String getIdentifier() {
            return "onlinetime";
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
                default -> "Invalid placeholder";
            };
        }

        private String getCurrentServerTime(OfflinePlayer player) {
            try {
                CompletableFuture<String> future = plugin.getService()
                        .getPlayerTimeFormatted(player.getUniqueId(), ConfigKeys.SERVER.getString());
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception exception) {
                return "Loading...";
            }
        }

        private String getTotalTime(OfflinePlayer player) {
            try {
                CompletableFuture<String> future = plugin.getService()
                        .getTotalPlayerTimeFormatted(player.getUniqueId());
                return future.get(1, TimeUnit.SECONDS);
            } catch (Exception exception) {
                return "Loading...";
            }
        }

        private String getSessionTime(OfflinePlayer player) {
            try {
                CompletableFuture<String> future = plugin.getService()
                        .getCurrentSessionTimeFormatted(player.getUniqueId());
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
            if (args.length < 2) {
                return "0";
            }

            String timeType = args[1].toLowerCase();

            try {
                CompletableFuture<Long> future = switch (timeType) {
                    case "current" ->
                            plugin.getService().getPlayerTime(player.getUniqueId(), ConfigKeys.SERVER.getString());
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
