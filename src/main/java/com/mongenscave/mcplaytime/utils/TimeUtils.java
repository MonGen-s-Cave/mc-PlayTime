package com.mongenscave.mcplaytime.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class TimeUtils {
    @NotNull
    public String formatTime(long milliseconds) {
        if (milliseconds <= 0) return "0s";

        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);

        StringBuilder builder = new StringBuilder();

        if (days > 0) {
            builder.append(days).append("d ");
            hours %= 24;
        }

        if (hours > 0) {
            builder.append(hours).append("h ");
            minutes %= 60;
        }

        if (minutes > 0) {
            builder.append(minutes).append("m ");
            seconds %= 60;
        }

        if (seconds > 0 || builder.isEmpty()) builder.append(seconds).append("s");
        return builder.toString().trim();
    }

    @NotNull
    public String formatTimeDetailed(long milliseconds) {
        if (milliseconds <= 0) return "0 seconds";

        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);

        StringBuilder builder = new StringBuilder();

        if (days > 0) {
            builder.append(days).append(days == 1 ? " day" : " days");
            hours %= 24;
            if (hours > 0 || minutes > 0 || seconds > 0) builder.append(", ");
        }

        if (hours > 0) {
            builder.append(hours).append(hours == 1 ? " hour" : " hours");
            minutes %= 60;
            if (minutes > 0 || seconds > 0) builder.append(", ");
        }

        if (minutes > 0) {
            builder.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            seconds %= 60;
            if (seconds > 0) builder.append(", ");
        }

        if (seconds > 0 || builder.isEmpty()) builder.append(seconds).append(seconds == 1 ? " second" : " seconds");
        return builder.toString();
    }

    public long parseTime(@NotNull String timeString) {
        long totalMillis = 0;
        StringBuilder current = new StringBuilder();

        for (char c : timeString.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) current.append(c);
            else if (Character.isLetter(c)) {
                if (!current.isEmpty()) {
                    long amount = Long.parseLong(current.toString());
                    current.setLength(0);

                    switch (c) {
                        case 'd' -> totalMillis += TimeUnit.DAYS.toMillis(amount);
                        case 'h' -> totalMillis += TimeUnit.HOURS.toMillis(amount);
                        case 'm' -> totalMillis += TimeUnit.MINUTES.toMillis(amount);
                        case 's' -> totalMillis += TimeUnit.SECONDS.toMillis(amount);
                    }
                }
            }
        }

        return totalMillis;
    }
}
