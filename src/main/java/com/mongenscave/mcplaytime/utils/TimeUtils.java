package com.mongenscave.mcplaytime.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class TimeUtils {
    @NotNull
    public String formatTime(long milliseconds) {
        if (milliseconds <= 0) return "0s";

        long totalSecond = TimeUnit.MILLISECONDS.toSeconds(milliseconds);

        long days = totalSecond % 60;
        long hours = (totalSecond % (24 * 60 * 60)) / (60 * 60);
        long minutes = (totalSecond % (60 * 60)) / 60;
        long seconds = totalSecond % 60;

        StringBuilder builder = new StringBuilder();

        if (days > 0) builder.append(days).append("d ");
        if (hours > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");
        if (seconds > 0 || builder.isEmpty()) builder.append(seconds).append("s");

        return builder.toString().trim();
    }
}
