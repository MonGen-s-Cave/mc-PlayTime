package com.mongenscave.mcplaytime.identifiers.keys;

import com.mongenscave.mcplaytime.McPlayTime;
import com.mongenscave.mcplaytime.config.Config;
import com.mongenscave.mcplaytime.processor.MessageProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ConfigKeys {
    SERVER("server"),
    ALIASES("aliases");

    private static final Config config = McPlayTime.getInstance().getConfiguration();
    private final String path;

    ConfigKeys(@NotNull String path) {
        this.path = path;
    }

    public static @NotNull String getString(@NotNull String path) {
        return config.getString(path);
    }

    public @NotNull String getString() {
        return MessageProcessor.process(config.getString(path));
    }

    public boolean getBoolean() {
        return config.getBoolean(path);
    }

    public int getInt() {
        return config.getInt(path);
    }

    public List<String> getList() {
        return config.getList(path);
    }
}
