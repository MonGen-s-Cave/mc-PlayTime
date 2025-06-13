package com.mongenscave.mcplaytime.utils;

import com.mongenscave.mcplaytime.McPlayTime;
import com.mongenscave.mcplaytime.commands.CommandPlayTime;
import com.mongenscave.mcplaytime.handler.CommandExceptionHandler;
import com.mongenscave.mcplaytime.identifiers.keys.ConfigKeys;
import lombok.experimental.UtilityClass;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.orphan.Orphans;

@UtilityClass
public class RegisterUtils {
    public void registerCommands() {
        var lamp = BukkitLamp.builder(McPlayTime.getInstance())
                .exceptionHandler(new CommandExceptionHandler())
                .build();

        lamp.register(Orphans.path(ConfigKeys.ALIASES.getList().toArray(String[]::new)).handler(new CommandPlayTime()));
    }
}
