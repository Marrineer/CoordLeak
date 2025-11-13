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
     * Sends a formatted message to a CommandSender using the MessageManager.
     *
     * @param sender The recipient of the message.
     * @param key The key of the message in messages.yml.
     * @param replacements Placeholders and their values.
     */
    public void send(CommandSender sender, String key, String... replacements) {
        plugin.getMessageManager().send(sender, key, replacements);
    }

    /**
     * Sends a formatted message to a Player using the MessageManager.
     *
     * @param player The recipient of the message.
     * @param key The key of the message in messages.yml.
     * @param replacements Placeholders and their values.
     */
    public void send(Player player, String key, String... replacements) {
        plugin.getMessageManager().send(player, key, replacements);
    }

    /**
     * Sends a list of formatted messages to a CommandSender using the MessageManager.
     *
     * @param sender The recipient of the messages.
     * @param key The key of the message list in messages.yml.
     * @param replacements Placeholders and their values.
     */
    public void sendList(CommandSender sender, String key, String... replacements) {
        plugin.getMessageManager().sendList(sender, key, replacements);
    }
}
