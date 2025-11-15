package com.qhuy.coordLeak.utils;

/**
 * Central permission constants to prevent typos and ensure consistency.
 */
public final class Permissions {
    
    private Permissions() {
        throw new AssertionError("Utility class");
    }
    
    // Core command permissions
    public static final String LEAK = "coordleak.leak";
    public static final String SHARE = "coordleak.share";
    public static final String ADMIN = "coordleak.admin";
    public static final String SETPRICE = "coordleak.setprice";
    
    // Bypass permissions
    public static final String BYPASS_COOLDOWN = "coordleak.bypass.cooldown";
    public static final String BYPASS_RATELIMIT = "coordleak.bypass.ratelimit";
    public static final String BYPASS_DAILYLIMIT = "coordleak.bypass.dailylimit";
    public static final String BYPASS_ALL = "coordleak.bypass.*";
    
    // Wildcard permissions
    public static final String ALL = "coordleak.*";
    
    /**
     * Checks if a permission string is valid.
     * @param permission Permission to validate
     * @return true if permission follows naming convention
     */
    public static boolean isValid(String permission) {
        return permission != null && permission.startsWith("coordleak.");
    }
}
