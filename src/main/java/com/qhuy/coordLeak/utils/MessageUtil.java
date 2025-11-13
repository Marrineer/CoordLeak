package com.qhuy.coordLeak.utils;

import com.qhuy.coordLeak.CoordLeak;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {
    private final CoordLeak plugin;

    public MessageUtil(CoordLeak plugin) {
        this.plugin = plugin;
    }

    private String formatMessage(String text, Player player) {
        String prefix = plugin.getConfig().getString("prefix", "");
        String message = String.format("%s %s", prefix, text);
        if (plugin.hasPAPI() && player != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

    public void sendToSender(String text, CommandSender sender) {
        String formattedMessage = formatMessage(text, sender instanceof Player ? (Player) sender : null);
        plugin.audience(sender).sendMessage(MiniMessage.miniMessage().deserialize(formattedMessage));
    }

    public void sendToPlayer(String text, Player player) {
        String formattedMessage = formatMessage(text, player);
        plugin.audience(player).sendMessage(MiniMessage.miniMessage().deserialize(formattedMessage));
    }
}
