package com.qhuy.coordLeak.utils;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

/**
 * Centralized utility for safely extracting player information.
 * Prevents NPE and provides consistent fallbacks across the plugin.
 */
public final class PlayerUtil {
    
    private PlayerUtil() {
        throw new AssertionError("Utility class");
    }
    
    /**
     * Safely extracts IP address from player.
     * @param player Player to extract IP from
     * @return IP address or "unknown" if unavailable
     */
    @NotNull
    public static String getPlayerIp(@Nullable Player player) {
        if (player == null) {
            return "unknown";
        }
        
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) {
            return "unknown";
        }
        
        return address.getAddress().getHostAddress();
    }
    
    /**
     * Gets IP with privacy consideration for logging.
     * @param player Player to extract IP from
     * @param logIpEnabled Whether IP logging is enabled
     * @return IP address or "REDACTED" if logging disabled
     */
    @NotNull
    public static String getLoggableIp(@Nullable Player player, boolean logIpEnabled) {
        if (!logIpEnabled) {
            return "REDACTED";
        }
        return getPlayerIp(player);
    }
    
    /**
     * Safely gets player name.
     * @param player Player to get name from
     * @return Player name or "Console" if null
     */
    @NotNull
    public static String getSafeName(@Nullable Player player) {
        return player != null ? player.getName() : "Console";
    }
    
    /**
     * Checks if player is still valid (online and world loaded).
     * @param player Player to check
     * @return true if player is safe to interact with
     */
    public static boolean isPlayerValid(@Nullable Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        
        // Ensure world is loaded - critical for Paper async chunk loading
        try {
            return player.getWorld() != null && player.getLocation() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
