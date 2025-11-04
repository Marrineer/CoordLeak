package com.qhuy.coordLeak.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.UUID;

public class CooldownManager implements Listener {
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    public void setCooldown(Player player, long seconds) {
        long cooldownEnd = System.currentTimeMillis() + (seconds * 1000);
        cooldowns.put(player.getUniqueId(), cooldownEnd);
    }

    public long getCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) return 0;

        long remaining = (cooldowns.get(uuid) - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }

    public boolean isOnCooldown(Player player) {
        return getCooldown(player) > 0;
    }

    public boolean usable(Player player, long seconds) {
        if(isOnCooldown(player)) {
            return false;
        } else {
            setCooldown(player, seconds);
            return true;
        }
    }

    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }
}
