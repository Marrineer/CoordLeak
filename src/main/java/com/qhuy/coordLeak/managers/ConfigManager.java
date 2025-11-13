package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConfigManager {

    private final CoordLeak plugin;

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
    private boolean auditLogSensitive;

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

        // Price Settings
        defaultPrice = config.getDouble("price.default", 50.0);
        minPrice = config.getDouble("price.min", 1.0);
        maxPrice = config.getDouble("price.max", 10000.0);

        // Cooldowns (per-command)
        leakCooldown = config.getLong("cooldowns.leak", 5) * 1000L;
        shareCooldown = config.getLong("cooldowns.share", 2) * 1000L;
        setPriceCooldown = config.getLong("cooldowns.setprice", 5) * 1000L;

        // Rate Limits (per-command, per-player)
        leakRateLimit = config.getInt("ratelimit.leak.limit", 5);
        leakRateLimitWindow = config.getLong("ratelimit.leak.window", TimeUnit.SECONDS.toMillis(60));
        shareRateLimit = config.getInt("ratelimit.share.limit", 10);
        shareRateLimitWindow = config.getLong("ratelimit.share.window", TimeUnit.SECONDS.toMillis(30));
        setPriceRateLimit = config.getInt("ratelimit.setprice.limit", 1);
        setPriceRateLimitWindow = config.getLong("ratelimit.setprice.window", TimeUnit.SECONDS.toMillis(10));

        // Global Rate Limit
        globalRateLimitEnabled = config.getBoolean("global.ratelimit.enabled", true);
        globalRateLimit = config.getInt("global.ratelimit.limit", 200);
        globalRateLimitWindow = config.getLong("global.ratelimit.window", TimeUnit.MINUTES.toMillis(1));
        globalRateLimitBlockDuration = config.getLong("global.ratelimit.block-duration", TimeUnit.SECONDS.toMillis(10));

        // Daily Limits (per-command, per-player)
        dailyLeakLimit = config.getInt("limits.daily.leak", 50);
        dailyShareLimit = config.getInt("limits.daily.share", 100);

        // Audit Logging
        auditLoggingEnabled = config.getBoolean("audit.enabled", true);
        auditLogFile = config.getString("audit.log-file", "logs/coordleak.log");
        auditLogSensitive = config.getBoolean("audit.log-sensitive", false);

        // Reload/Admin Command Protection
        reloadRequireConfirm = config.getBoolean("reload.require-confirm", true);
        confirmationTimeout = config.getLong("reload.confirmation-timeout", TimeUnit.SECONDS.toMillis(10));

        // Blacklist/Whitelist
        blacklistEnabled = config.getBoolean("blacklist.enabled", false);
        blacklistedUUIDs = config.getStringList("blacklist.uuids");
        whitelistEnabled = config.getBoolean("whitelist.enabled", false);
        whitelistedUUIDs = config.getStringList("whitelist.uuids");

        // Target Validation
        excludedWorlds = config.getStringList("target.exclude-worlds");
        excludedPermissions = config.getStringList("target.exclude-permissions");
        requireConsentForShare = config.getBoolean("target.require-consent-for-share", false);
    }

    // --- Getters ---

    public double getDefaultPrice() {
        return defaultPrice;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setDefaultPrice(double price) {
        this.defaultPrice = price;
        plugin.getConfig().set("price.default", price);
        plugin.saveConfig();
    }

    public long getCooldown(String command) {
        switch (command.toLowerCase()) {
            case "leak": return leakCooldown;
            case "share": return shareCooldown;
            case "setprice": return setPriceCooldown;
            default: return 0;
        }
    }

    public int getRateLimit(String command) {
        switch (command.toLowerCase()) {
            case "leak": return leakRateLimit;
            case "share": return shareRateLimit;
            case "setprice": return setPriceRateLimit;
            default: return 0;
        }
    }

    public long getRateLimitWindow(String command) {
        switch (command.toLowerCase()) {
            case "leak": return leakRateLimitWindow;
            case "share": return shareRateLimitWindow;
            case "setprice": return setPriceRateLimitWindow;
            default: return 0;
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
            case "leak": return dailyLeakLimit;
            case "share": return dailyShareLimit;
            default: return 0;
        }
    }

    public boolean isAuditLoggingEnabled() {
        return auditLoggingEnabled;
    }

    public String getAuditLogFile() {
        return auditLogFile;
    }

    public boolean isAuditLogSensitiveEnabled() {
        return auditLogSensitive;
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
