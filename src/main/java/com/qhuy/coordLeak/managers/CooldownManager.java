package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CooldownManager {
    private final CoordLeak plugin;
    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public CooldownManager(CoordLeak plugin) {
        this.plugin = plugin;
    }

    public void setCooldown(Player player, long seconds) {
        long cooldownEnd = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
        cooldowns.put(player.getUniqueId(), cooldownEnd);
    }

    public long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        Long endTime = cooldowns.get(uuid);
        if (endTime == null || System.currentTimeMillis() >= endTime) {
            return 0;
        }
        return TimeUnit.MILLISECONDS.toSeconds(endTime - System.currentTimeMillis());
    }

    public boolean isOnCooldown(Player player) {
        return getRemainingCooldown(player) > 0;
    }

    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }
}
