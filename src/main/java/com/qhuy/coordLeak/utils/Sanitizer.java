package com.qhuy.coordLeak.utils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/**
 * Sanitizes user input to prevent injection attacks.
 */
public class Sanitizer {

    public static String sanitizeCustomText(String input, Player player, boolean hasAdminPerm) {
        if (input == null || input.isEmpty()) return input;

        if (hasAdminPerm) {
            return input; // Admins can use MiniMessage tags
        }

        // Strip MiniMessage tags for non-admins
        String stripped = MiniMessage.miniMessage().stripTags(input);

        // Escape PlaceholderAPI placeholders (% to %%)
        stripped = stripped.replace("%", "%%");

        // Remove control characters
        stripped = stripped.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // Limit length
        if (stripped.length() > 256) {
            stripped = stripped.substring(0, 256);
        }

        return stripped;
    }

    public static boolean containsDangerousPatterns(String input) {
        if (input == null) return false;

        // Check for suspicious patterns
        String lower = input.toLowerCase();
        return lower.contains("<script")
                || lower.contains("javascript:")
                || lower.contains("onerror=")
                || lower.contains("<iframe");
    }
}
