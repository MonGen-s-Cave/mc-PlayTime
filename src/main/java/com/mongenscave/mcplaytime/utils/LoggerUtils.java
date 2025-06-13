package com.mongenscave.mcplaytime.utils;

import com.mongenscave.mcplaytime.McPlayTime;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LoggerUtils {
    private final Logger logger = LogManager.getLogger("McPlayTime");

    public void info(@NotNull String msg, @NotNull Object... objs) {
        logger.info(msg, objs);
    }

    public void warn(@NotNull String msg, @NotNull Object... objs) {
        logger.warn(msg, objs);
    }

    public void error(@NotNull String msg, @NotNull Object... objs) {
        logger.error(msg, objs);
    }

    public void printStartup() {
        String yellow = "\u001B[33m";
        String reset = "\u001B[0m";
        String software = McPlayTime.getInstance().getServer().getName();
        String version = McPlayTime.getInstance().getServer().getVersion();

        String asciiArt = yellow +
                "  _____  _          _______ _                \n" + reset +
                yellow + " |  __ \\| |        |__   __(_)               \n" + reset +
                yellow + " | |__) | | __ _ _   _| |   _ _ __ ___   ___\n" + reset +
                yellow + " |  ___/| |/ _` | | | | |  | | '_ ` _ \\ / _ \\\n" + reset +
                yellow + " | |    | | (_| | |_| | |  | | | | | | |  __/\n" + reset +
                yellow + " |_|    |_|\\__,_|\\__, |_|  |_|_| |_| |_|\\___|" + reset +
                yellow + "\n                    |___/                       " + reset;

        info(" ");
        String[] lines = asciiArt.split("\n");

        for (String line : lines) {
            info(line);
        }

        info(" ");
        info("{}   The plugin successfully started.{}", yellow, reset);
        info("{}   mc-PlayTime {} {}{}", yellow, software, version, reset);
        info("{}   Discord @ dc.mongenscave.com{}", yellow, reset);
        info(" ");
    }
}