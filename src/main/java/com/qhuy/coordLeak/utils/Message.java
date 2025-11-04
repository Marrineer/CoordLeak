package com.qhuy.coordLeak.utils;

import com.qhuy.coordLeak.CoordLeak;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Message {
    private static final String PREFIX = CoordLeak.getInstance().getConfig().getString("prefix", "");

    public static String get(String placeholder) {
        return CoordLeak.getInstance().getMessage().getString(
                placeholder,
                CoordLeak.getInstance().getMessage().getDefaults().getString(
                        placeholder,
                        "Message not found"));
    }

    public static void sendToSender(String text, CommandSender sender) {
        if (sender instanceof Player player) {
            CoordLeak.getInstance().audience(player).sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            PlaceholderAPI.setPlaceholders(
                                    player,
                                    String.format("%s %s", PREFIX, text)
                            )
                    )
            );
        } else {
            CoordLeak.getInstance().audience(sender).sendMessage(
                    MiniMessage.miniMessage().deserialize(PREFIX + text)
            );
        }
    }

    public static void sendToPlayer(String text, Player player) {
        CoordLeak.getInstance().audience(player).sendMessage(
                MiniMessage.miniMessage().deserialize(
                        PlaceholderAPI.setPlaceholders(
                                player,
                                String.format("%s %s", PREFIX, text)
                        )
                )
        );
    }
}
