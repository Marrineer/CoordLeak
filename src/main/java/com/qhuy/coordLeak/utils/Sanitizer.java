package com.qhuy.coordLeak.utils;

import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Sanitizes user input to prevent formatting abuse.
 */
public class Sanitizer {

    /**
     * Sanitizes a string by stripping MiniMessage tags and escaping PlaceholderAPI placeholders.
     * This is intended for user-provided text from non-admins.
     *
     * @param input The string to sanitize.
     * @return The sanitized string.
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Strip MiniMessage tags to prevent formatting abuse.
        String sanitized = MiniMessage.miniMessage().stripTags(input);

        // Escape PlaceholderAPI placeholders to prevent them from being parsed.
        sanitized = sanitized.replace("%", "%%");

        return sanitized;
    }
}
