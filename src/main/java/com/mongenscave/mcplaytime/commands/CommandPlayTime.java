package com.mongenscave.mcplaytime.commands;

import com.mongenscave.mcplaytime.McPlayTime;
import com.mongenscave.mcplaytime.identifiers.keys.ConfigKeys;
import com.mongenscave.mcplaytime.identifiers.keys.MessageKeys;
import com.mongenscave.mcplaytime.processor.MessageProcessor;
import com.mongenscave.mcplaytime.utils.LoggerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

import java.util.concurrent.CompletableFuture;

public class CommandPlayTime implements OrphanCommand {
    private static final McPlayTime plugin = McPlayTime.getInstance();

    @Subcommand("reload")
    @CommandPermission("mcplaytime.reload")
    public void reload(@NotNull CommandSender sender) {
        plugin.getLanguage().reload();
        plugin.getConfiguration().reload();
        sender.sendMessage(MessageKeys.RELOAD.getMessage());
    }

    @CommandPlaceholder
    public void onlineTime(@NotNull Player player) {
        CompletableFuture<String> serverTimeFuture = plugin.getService().getPlayerTimeFormatted(player.getUniqueId(), ConfigKeys.SERVER.getString());
        CompletableFuture<String> totalTimeFuture = plugin.getService().getTotalPlayerTimeFormatted(player.getUniqueId());

        CompletableFuture.allOf(serverTimeFuture, totalTimeFuture)
                .thenRun(() -> {
                    try {
                        String serverTime = serverTimeFuture.get();
                        String totalTime = totalTimeFuture.get();

                        for (String message : MessageKeys.ONLINE_TIME.getMessages()) {
                            message = message.replace("{time}", serverTime).replace("{totalTime}", totalTime);

                            player.sendMessage(MessageProcessor.process(message));
                        }
                    } catch (Exception exception) {
                        LoggerUtils.error(exception.getMessage());
                    }
                });
    }
}
