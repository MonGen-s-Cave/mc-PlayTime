package com.mongenscave.mcplaytime.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PlayerData {
    private UUID uuid;
    private String username;
    private String server;
    private long totalTime;
    private long sessionStart;
    private long lastSeen;

    public boolean isOnline() {
        return sessionStart > 0;
    }

    public long getCurrentSessionTime() {
        if (!isOnline()) return 0;
        return System.currentTimeMillis() - sessionStart;
    }

    public long getTotalTimeWithCurrentSession() {
        return totalTime + getCurrentSessionTime();
    }
}
