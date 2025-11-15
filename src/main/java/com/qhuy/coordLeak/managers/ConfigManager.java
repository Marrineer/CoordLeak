package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConfigManager {

    private final CoordLeak plugin;

    private String prefix;

    // Price Settings
    private double defaultPrice;
    private double minPrice;
    private double maxPrice;

    // Cooldowns (per-command)
    private long leakCooldown;
    private long shareCooldown;
    private long setPriceCooldown;

    // Rate Limits (per-command, per-player)
    private int leakRateLimit;
    private long leakRateLimitWindow;
    private int shareRateLimit;
    private long shareRateLimitWindow;
    private int setPriceRateLimit;
    private long setPriceRateLimitWindow;

    // Global Rate Limit
    private boolean globalRateLimitEnabled;
    private int globalRateLimit;
    private long globalRateLimitWindow;
    private long globalRateLimitBlockDuration;

    // Daily Limits (per-command, per-player)
    private int dailyLeakLimit;
    private int dailyShareLimit;

    // Audit Logging
    private boolean auditLoggingEnabled;
    private String auditLogFile;
    private boolean ipLoggingEnabled;

    // Reload/Admin Command Protection
    private boolean reloadRequireConfirm;
    private long confirmationTimeout;

    // Blacklist/Whitelist
    private boolean blacklistEnabled;
    private List<String> blacklistedUUIDs;
    private boolean whitelistEnabled;
    private List<String> whitelistedUUIDs;

    // Target Validation
    private List<String> excludedWorlds;
    private List<String> excludedPermissions;
    private boolean requireConsentForShare;


    public ConfigManager(CoordLeak plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        // Save defaults if config is empty
        plugin.saveDefaultConfig();

        // General Settings with validation
        prefix = config.getString("prefix", "<i><gradient:#FFFFFF:#29E7D7>[ Coord ]</gradient></i>");
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "<i><gradient:#FFFFFF:#29E7D7>[ Coord ]</gradient></i>";
            plugin.getLogger().warning("Invalid prefix in config, using default");
        }

        // Price Settings with validation
        defaultPrice = Math.max(0, config.getDouble("price.default", 50.0));
        minPrice = Math.max(0, config.getDouble("price.min", 1.0));
        maxPrice = Math.max(minPrice, config.getDouble("price.max", 100000.0));
        
        // Ensure price bounds are logical
        if (defaultPrice < minPrice) defaultPrice = minPrice;
        if (defaultPrice > maxPrice) defaultPrice = maxPrice;

        // Cooldowns (per-command) - ensure non-negative
        leakCooldown = Math.max(0, config.getLong("cooldowns.leak", 5)) * 1000L;
        shareCooldown = Math.max(0, config.getLong("cooldowns.share", 2)) * 1000L;
        setPriceCooldown = Math.max(0, config.getLong("cooldowns.setprice", 5)) * 1000L;

        // Rate Limits (per-command, per-player) - ensure positive values
        leakRateLimit = Math.max(1, config.getInt("ratelimit.leak.limit", 5));
        leakRateLimitWindow = Math.max(1000, config.getLong("ratelimit.leak.window", TimeUnit.SECONDS.toMillis(60)));
        shareRateLimit = Math.max(1, config.getInt("ratelimit.share.limit", 10));
        shareRateLimitWindow = Math.max(1000, config.getLong("ratelimit.share.window", TimeUnit.SECONDS.toMillis(30)));
        setPriceRateLimit = Math.max(1, config.getInt("ratelimit.setprice.limit", 1));
        setPriceRateLimitWindow = Math.max(1000, config.getLong("ratelimit.setprice.window", TimeUnit.SECONDS.toMillis(10)));

        // Global Rate Limit - ensure reasonable values
        globalRateLimitEnabled = config.getBoolean("global.ratelimit.enabled", true);
        globalRateLimit = Math.max(1, config.getInt("global.ratelimit.limit", 200));
        globalRateLimitWindow = Math.max(1000, config.getLong("global.ratelimit.window", TimeUnit.MINUTES.toMillis(1)));
        globalRateLimitBlockDuration = Math.max(1000, config.getLong("global.ratelimit.block-duration", TimeUnit.SECONDS.toMillis(10)));

        // Daily Limits (per-command, per-player) - 0 means unlimited
        dailyLeakLimit = Math.max(0, config.getInt("limits.daily.leak", 50));
        dailyShareLimit = Math.max(0, config.getInt("limits.daily.share", 100));

        // Audit Logging
        auditLoggingEnabled = config.getBoolean("audit.enabled", true);
        auditLogFile = config.getString("audit.log-file", "logs/coordleak.log");

        if (config.isSet("audit.log-ip-address")) {
            ipLoggingEnabled = config.getBoolean("audit.log-ip-address");
        } else if (config.isSet("audit.log-sensitive")) {
            ipLoggingEnabled = config.getBoolean("audit.log-sensitive");
            plugin.getLogger().warning("Config key 'audit.log-sensitive' is deprecated. Please migrate to 'audit.log-ip-address'.");
        } else {
            ipLoggingEnabled = false;
        }

        // Reload/Admin Command Protection
        reloadRequireConfirm = config.getBoolean("reload.require-confirm", true);
        confirmationTimeout = Math.max(1000, config.getLong("reload.confirmation-timeout", TimeUnit.SECONDS.toMillis(10)));

        // Blacklist/Whitelist - handle null lists
        blacklistEnabled = config.getBoolean("blacklist.enabled", false);
        List<String> tempBlacklist = config.getStringList("blacklist.uuids");
        blacklistedUUIDs = tempBlacklist != null ? tempBlacklist : new java.util.ArrayList<>();
        
        whitelistEnabled = config.getBoolean("whitelist.enabled", false);
        List<String> tempWhitelist = config.getStringList("whitelist.uuids");
        whitelistedUUIDs = tempWhitelist != null ? tempWhitelist : new java.util.ArrayList<>();

        // Target Validation - handle null lists
        List<String> tempWorlds = config.getStringList("target.exclude-worlds");
        excludedWorlds = tempWorlds != null ? tempWorlds : new java.util.ArrayList<>();
        
        List<String> tempPerms = config.getStringList("target.exclude-permissions");
        excludedPermissions = tempPerms != null ? tempPerms : new java.util.ArrayList<>();
        
        requireConsentForShare = config.getBoolean("target.require-consent-for-share", false);
        
        plugin.getLogger().info("Configuration loaded successfully");
    }

    // --- Getters ---

    public String getPrefix() {
        return prefix;
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(double price) {
        this.defaultPrice = price;
        plugin.getConfig().set("price.default", price);
        plugin.saveConfig();
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    /**
     * Returns cooldown in milliseconds for given command key.
     * Backward compatibility: existing internal fields already stored in ms.
     */
    public long getCooldownMillis(String command) {
        return switch (command.toLowerCase()) {
            case "leak" -> leakCooldown;
            case "share" -> shareCooldown;
            case "setprice" -> setPriceCooldown;
            default -> 0L;
        };
    }

    @Deprecated // Use getCooldownMillis instead
    public long getCooldown(String command) {
        return getCooldownMillis(command);
    }

    public int getRateLimit(String command) {
        switch (command.toLowerCase()) {
            case "leak":
                return leakRateLimit;
            case "share":
                return shareRateLimit;
            case "setprice":
                return setPriceRateLimit;
            default:
                return 0;
        }
    }

    public long getRateLimitWindow(String command) {
        switch (command.toLowerCase()) {
            case "leak":
                return leakRateLimitWindow;
            case "share":
                return shareRateLimitWindow;
            case "setprice":
                return setPriceRateLimitWindow;
            default:
                return 0;
        }
    }

    public boolean isGlobalRateLimitEnabled() {
        return globalRateLimitEnabled;
    }

    public int getGlobalRateLimit() {
        return globalRateLimit;
    }

    public long getGlobalRateLimitWindow() {
        return globalRateLimitWindow;
    }

    public long getGlobalRateLimitBlockDuration() {
        return globalRateLimitBlockDuration;
    }

    public int getDailyLimit(String command) {
        switch (command.toLowerCase()) {
            case "leak":
                return dailyLeakLimit;
            case "share":
                return dailyShareLimit;
            default:
                return 0;
        }
    }

    public boolean isAuditLoggingEnabled() {
        return auditLoggingEnabled;
    }

    public String getAuditLogFile() {
        return auditLogFile;
    }

    public boolean isIpLoggingEnabled() {
        return ipLoggingEnabled;
    }

    public boolean isReloadRequireConfirm() {
        return reloadRequireConfirm;
    }

    public long getConfirmationTimeout(String command) {
        // Currently, only reload uses this, but can be extended
        return confirmationTimeout;
    }

    public boolean isBlacklistEnabled() {
        return blacklistEnabled;
    }

    public List<String> getBlacklistedUUIDs() {
        return Collections.unmodifiableList(blacklistedUUIDs);
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public List<String> getWhitelistedUUIDs() {
        return Collections.unmodifiableList(whitelistedUUIDs);
    }

    public List<String> getExcludedWorlds() {
        return Collections.unmodifiableList(excludedWorlds);
    }

    public List<String> getExcludedPermissions() {
        return Collections.unmodifiableList(excludedPermissions);
    }

    public boolean isConsentRequiredForShare() {
        return requireConsentForShare;
    }
}
