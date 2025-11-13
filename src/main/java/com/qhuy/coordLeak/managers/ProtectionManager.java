package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtectionManager {

    private final CoordLeak plugin;
    private final ConfigManager configManager;

    // Cooldowns: Map<UUID, Map<CommandName, LastUsedTimestamp>>
    private final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();

    // Rate Limits: Map<UUID, Map<CommandName, Deque<Timestamp>>> (Sliding Window)
    private final Map<UUID, Map<String, Deque<Long>>> playerRateLimits = new ConcurrentHashMap<>();

    // Daily Limits: Map<UUID, Map<CommandName, Map<Date, Count>>>
    private final Map<UUID, Map<String, Map<Long, AtomicInteger>>> playerDailyLimits = new ConcurrentHashMap<>();
    private final AtomicInteger globalRequestCount = new AtomicInteger(0);
    // Command Confirmation (for reload/setprice)
    // Map<UUID, Map<CommandName, ConfirmationTimestamp>>
    private final Map<UUID, Map<String, Long>> pendingConfirmations = new ConcurrentHashMap<>();
    // Global Rate Limit
    private long globalLastResetTime = System.currentTimeMillis();
    private long globalBlockUntil = 0; // Timestamp until global requests are blocked

    public ProtectionManager(CoordLeak plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Checks if a player is on cooldown for a specific command.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name (e.g., "leak", "share", "reload", "setprice").
     * @return True if the player is on cooldown, false otherwise.
     */
    public boolean isOnCooldown(UUID playerUUID, String command) {
        if (hasBypassPermission(playerUUID, "cooldown")) return false;

        long cooldownMillis = configManager.getCooldown(command);
        if (cooldownMillis <= 0) return false; // No cooldown configured

        Map<String, Long> commandCooldowns = playerCooldowns.getOrDefault(playerUUID, new ConcurrentHashMap<>());
        long lastUsed = commandCooldowns.getOrDefault(command, 0L);
        long remaining = (lastUsed + cooldownMillis) - System.currentTimeMillis();

        return remaining > 0;
    }

    /**
     * Applies cooldown for a player and command.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name.
     */
    public void applyCooldown(UUID playerUUID, String command) {
        playerCooldowns.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()).put(command, System.currentTimeMillis());
    }

    /**
     * Gets the remaining cooldown time for a player and command.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name.
     * @return Remaining cooldown in milliseconds.
     */
    public long getRemainingCooldown(UUID playerUUID, String command) {
        if (hasBypassPermission(playerUUID, "cooldown")) return 0;

        long cooldownMillis = configManager.getCooldown(command);
        if (cooldownMillis <= 0) return 0;

        Map<String, Long> commandCooldowns = playerCooldowns.getOrDefault(playerUUID, new ConcurrentHashMap<>());
        long lastUsed = commandCooldowns.getOrDefault(command, 0L);
        return Math.max(0, (lastUsed + cooldownMillis) - System.currentTimeMillis());
    }

    /**
     * Checks if a player exceeds the rate limit for a specific command.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name.
     * @return True if the player exceeds the rate limit, false otherwise.
     */
    public boolean isRateLimited(UUID playerUUID, String command) {
        if (hasBypassPermission(playerUUID, "ratelimit")) return false;

        int limit = configManager.getRateLimit(command);
        long windowMillis = configManager.getRateLimitWindow(command);

        if (limit <= 0 || windowMillis <= 0) return false; // No rate limit configured

        Map<String, Deque<Long>> commandRateLimits = playerRateLimits.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
        Deque<Long> timestamps = commandRateLimits.computeIfAbsent(command, k -> new ConcurrentLinkedDeque<>());

        long currentTime = System.currentTimeMillis();
        timestamps.removeIf(timestamp -> timestamp < currentTime - windowMillis); // Clean old entries

        return timestamps.size() >= limit;
    }

    /**
     * Records a command usage for rate limiting.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name.
     */
    public void recordRateLimitUsage(UUID playerUUID, String command) {
        Map<String, Deque<Long>> commandRateLimits = playerRateLimits.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
        Deque<Long> timestamps = commandRateLimits.computeIfAbsent(command, k -> new ConcurrentLinkedDeque<>());
        timestamps.addLast(System.currentTimeMillis());
    }

    /**
     * Checks and applies global rate limit.
     *
     * @return True if global rate limit is exceeded and requests should be blocked.
     */
    public boolean isGlobalRateLimited() {
        long currentTime = System.currentTimeMillis();
        long windowMillis = configManager.getGlobalRateLimitWindow(); // e.g., 60 seconds
        int limit = configManager.getGlobalRateLimit(); // e.g., 200 requests

        if (limit <= 0 || windowMillis <= 0) return false;

        // If currently blocked, check if block period has expired
        if (globalBlockUntil > currentTime) {
            return true;
        }

        // Reset count if window has passed
        if (currentTime - globalLastResetTime > windowMillis) {
            globalRequestCount.set(0);
            globalLastResetTime = currentTime;
            globalBlockUntil = 0; // Ensure block is lifted if window passed
        }

        // Increment and check
        int currentCount = globalRequestCount.incrementAndGet();
        if (currentCount > limit) {
            long blockDuration = configManager.getGlobalRateLimitBlockDuration(); // e.g., 10 seconds
            globalBlockUntil = currentTime + blockDuration;
            plugin.getLogger().warning("Global rate limit exceeded! Blocking new requests for " + (blockDuration / 1000) + " seconds.");
            return true;
        }
        return false;
    }

    /**
     * Checks if a player has exceeded their daily limit for a command.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name (e.g., "leak").
     * @return True if the daily limit is exceeded, false otherwise.
     */
    public boolean hasExceededDailyLimit(UUID playerUUID, String command) {
        if (hasBypassPermission(playerUUID, "dailylimit")) return false;

        int dailyLimit = configManager.getDailyLimit(command);
        if (dailyLimit <= 0) return false; // No daily limit configured

        Map<String, Map<Long, AtomicInteger>> playerCommandLimits = playerDailyLimits.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
        Map<Long, AtomicInteger> commandDailyLimits = playerCommandLimits.computeIfAbsent(command, k -> new ConcurrentHashMap<>());

        long currentDay = TimeUnit.DAYS.toMillis(System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1)); // Start of current day
        AtomicInteger count = commandDailyLimits.computeIfAbsent(currentDay, k -> new AtomicInteger(0));

        return count.get() >= dailyLimit;
    }

    /**
     * Increments the daily usage count for a player and command.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name.
     */
    public void incrementDailyUsage(UUID playerUUID, String command) {
        Map<String, Map<Long, AtomicInteger>> playerCommandLimits = playerDailyLimits.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
        Map<Long, AtomicInteger> commandDailyLimits = playerCommandLimits.computeIfAbsent(command, k -> new ConcurrentHashMap<>());

        long currentDay = TimeUnit.DAYS.toMillis(System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1));
        commandDailyLimits.computeIfAbsent(currentDay, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Checks if a player is blacklisted.
     *
     * @param playerUUID The UUID of the player.
     * @return True if blacklisted, false otherwise.
     */
    public boolean isBlacklisted(UUID playerUUID) {
        return configManager.getBlacklistedUUIDs().contains(playerUUID.toString());
    }

    /**
     * Checks if a player is whitelisted (if whitelist is enabled).
     *
     * @param playerUUID The UUID of the player.
     * @return True if whitelisted or whitelist is not enabled, false if whitelist is enabled and player is not in it.
     */
    public boolean isWhitelisted(UUID playerUUID) {
        if (!configManager.isWhitelistEnabled()) {
            return true; // Whitelist not enabled, all are allowed
        }
        return configManager.getWhitelistedUUIDs().contains(playerUUID.toString());
    }

    /**
     * Checks if a player has a pending confirmation for a command.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name (e.g., "reload", "setprice").
     * @return True if a confirmation is pending and within the timeout, false otherwise.
     */
    public boolean hasPendingConfirmation(UUID playerUUID, String command) {
        Map<String, Long> confirmations = pendingConfirmations.get(playerUUID);
        if (confirmations == null) return false;

        Long timestamp = confirmations.get(command);
        if (timestamp == null) return false;

        long timeout = configManager.getConfirmationTimeout(command);
        return (System.currentTimeMillis() - timestamp) < timeout;
    }

    /**
     * Initiates a confirmation for a command.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name.
     */
    public void initiateConfirmation(UUID playerUUID, String command) {
        pendingConfirmations.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()).put(command, System.currentTimeMillis());
    }

    /**
     * Clears a pending confirmation for a command.
     *
     * @param playerUUID The UUID of the player.
     * @param command    The command name.
     */
    public void clearConfirmation(UUID playerUUID, String command) {
        Map<String, Long> confirmations = pendingConfirmations.get(playerUUID);
        if (confirmations != null) {
            confirmations.remove(command);
            if (confirmations.isEmpty()) {
                pendingConfirmations.remove(playerUUID);
            }
        }
    }

    /**
     * Checks if the target player is valid for leaking/sharing.
     *
     * @param targetPlayer The player to check.
     * @return True if the target is valid, false otherwise.
     */
    public boolean isValidTarget(Player targetPlayer) {
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            return false; // Must be online
        }

        // Excluded worlds
        if (configManager.getExcludedWorlds().contains(targetPlayer.getWorld().getName())) {
            return false;
        }

        // Excluded permissions
        for (String perm : configManager.getExcludedPermissions()) {
            if (targetPlayer.hasPermission(perm)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a player has a bypass permission.
     *
     * @param playerUUID The UUID of the player.
     * @param type       The type of bypass (e.g., "cooldown", "ratelimit", "dailylimit", "admin").
     * @return True if the player has the bypass permission.
     */
    public boolean hasBypassPermission(UUID playerUUID, String type) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return false; // Only online players can have permissions

        if (player.hasPermission("coordleak.admin")) {
            return true; // Admin bypasses everything
        }

        switch (type) {
            case "cooldown":
                return player.hasPermission("coordleak.bypass.cooldown");
            case "ratelimit":
                return player.hasPermission("coordleak.bypass.ratelimit");
            case "dailylimit":
                return player.hasPermission("coordleak.bypass.dailylimit");
            default:
                return false;
        }
    }

    /**
     * Cleans up expired entries from cooldowns, rate limits, and daily limits.
     * This should be scheduled periodically.
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();

        // Clean playerCooldowns (not strictly necessary as it's overwritten, but good for memory)
        playerCooldowns.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(cmdEntry -> {
                long cooldownMillis = configManager.getCooldownMillis(cmdEntry.getKey());
                return (cmdEntry.getValue() + cooldownMillis) < currentTime;
            });
            return entry.getValue().isEmpty();
        });

        // Clean playerRateLimits (sliding window)
        playerRateLimits.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(cmdEntry -> {
                long windowMillis = configManager.getRateLimitWindow(cmdEntry.getKey());
                cmdEntry.getValue().removeIf(timestamp -> timestamp < currentTime - windowMillis);
                return cmdEntry.getValue().isEmpty();
            });
            return entry.getValue().isEmpty();
        });

        // Clean playerDailyLimits (remove old days)
        long currentDay = TimeUnit.DAYS.toMillis(currentTime / TimeUnit.DAYS.toMillis(1));
        playerDailyLimits.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(cmdEntry -> {
                cmdEntry.getValue().entrySet().removeIf(dayEntry -> dayEntry.getKey() < currentDay);
                return cmdEntry.getValue().isEmpty();
            });
            return entry.getValue().isEmpty();
        });

        // Clean pendingConfirmations
        pendingConfirmations.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(cmdEntry -> {
                long timeout = configManager.getConfirmationTimeout(cmdEntry.getKey());
                return (cmdEntry.getValue() + timeout) < currentTime;
            });
            return entry.getValue().isEmpty();
        });
    }

    /**
     * Placeholder for target consent mechanism.
     *
     * @param targetUUID The UUID of the target player.
     * @return Always true for now, actual implementation would involve a pending request.
     */
    public boolean hasTargetConsent(UUID targetUUID) {
        // TODO: Implement actual consent mechanism (e.g., /coord accept/deny)
        return !configManager.isConsentRequiredForShare(); // If consent not required, always true
    }

    /**
     * Gets the current global block remaining time.
     *
     * @return Remaining time in milliseconds, 0 if not blocked.
     */
    public long getGlobalBlockRemaining() {
        return Math.max(0, globalBlockUntil - System.currentTimeMillis());
    }
}
