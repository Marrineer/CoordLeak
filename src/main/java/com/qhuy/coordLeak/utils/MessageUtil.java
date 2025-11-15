package com.qhuy.coordLeak.utils;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {
    private final CoordLeak plugin;

    public MessageUtil(CoordLeak plugin) {
        this.plugin = plugin;
    }

    /**
     * Formats a duration in milliseconds into a human-readable string (e.g., 1h 2m 3s).
     *
     * @param milliseconds The duration in milliseconds.
     * @return A formatted string representing the duration.
     */
    public static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        seconds %= 60;
        if (minutes < 60) {
            return minutes + "m " + seconds + "s";
        }
        long hours = minutes / 60;
        minutes %= 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    /**
     * Sends a formatted message to a CommandSender using the MessageManager.
     *
     * @param sender       The recipient of the message.
     * @param key          The key of the message in messages.yml.
     * @param replacements Placeholders and their values.
     */
    public void send(CommandSender sender, String key, String... replacements) {
        plugin.getMessageManager().send(sender, key, replacements);
    }

    /**
     * Sends a formatted message to a Player using the MessageManager.
     *
     * @param player       The recipient of the message.
     * @param key          The key of the message in messages.yml.
     * @param replacements Placeholders and their values.
     */
    public void send(Player player, String key, String... replacements) {
        plugin.getMessageManager().send(player, key, replacements);
    }

    /**
     * Sends a list of formatted messages to a CommandSender using the MessageManager.
     *
     * @param sender       The recipient of the messages.
     * @param key          The key of the message list in messages.yml.
     * @param replacements Placeholders and their values.
     */
    public void sendList(CommandSender sender, String key, String... replacements) {
        plugin.getMessageManager().sendList(sender, key, replacements);
    }
}
