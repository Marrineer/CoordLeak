package com.qhuy.coordLeak.utils;

import com.qhuy.coordLeak.CoordLeak;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Message {
    private static final String PREFIX = CoordLeak.getInstance().getConfig().getString("prefix", "");
    private static final boolean PAPIEnabled = CoordLeak.getInstance().hasPAPI();

    public static String get(String placeholder) {
        return CoordLeak.getInstance().getMessage().getString(
                placeholder,
                CoordLeak.getInstance().getMessage().getDefaults().getString(
                        placeholder,
                        "Message not found"));
    }

    public static void sendToSender(String text, CommandSender sender) {
        String message = String.format("%s %s", PREFIX, text);
        if (sender instanceof Player player) {
            if(PAPIEnabled) {
                message = PlaceholderAPI.setPlaceholders(player, message);
            }
            CoordLeak.getInstance().audience(sender).sendMessage(
                    MiniMessage.miniMessage().deserialize(message)
            );
        } else {
            CoordLeak.getInstance().audience(sender).sendMessage(
                    MiniMessage.miniMessage().deserialize(message)
            );
        }
    }

    public static void sendToPlayer(String text, Player player) {
        String message = String.format("%s %s", PREFIX, text);
        if(PAPIEnabled) {
            message = PlaceholderAPI.setPlaceholders(
                    player,
                    message
            );
        }
        CoordLeak.getInstance().audience(player).sendMessage(
                MiniMessage.miniMessage().deserialize(message)
        );
    }
}
