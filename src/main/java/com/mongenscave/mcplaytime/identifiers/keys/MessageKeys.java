package com.mongenscave.mcplaytime.identifiers.keys;

import com.mongenscave.mcplaytime.McPlayTime;
import com.mongenscave.mcplaytime.config.Config;
import com.mongenscave.mcplaytime.processor.MessageProcessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public enum MessageKeys {
    NO_PERMISSION("messages.no-permission"),
    PLAYER_REQUIRED("messages.player-required"),

    RELOAD("messages.reload"),

    ONLINE_TIME("messages.online-time");

    private static final Config language = McPlayTime.getInstance().getLanguage();
    private final String path;

    MessageKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getMessage() {
        return MessageProcessor.process(language.getString(path)).replace("%prefix%", MessageProcessor.process(language.getString("prefix")));
    }

    public List<String> getMessages() {
        return language.getStringList(path)
                .stream()
                .toList();
    }
}
