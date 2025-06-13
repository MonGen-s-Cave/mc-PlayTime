package com.mongenscave.mcplaytime.listener;

import com.mongenscave.mcplaytime.McPlayTime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {
    private static final McPlayTime plugin = McPlayTime.getInstance();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        plugin.getService().handlePlayerJoin(event.getPlayer()).exceptionally(throwable -> null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        plugin.getService().handlePlayerQuit(event.getPlayer()).exceptionally(throwable -> null);
    }
}
