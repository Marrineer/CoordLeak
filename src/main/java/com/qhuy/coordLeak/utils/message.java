package com.qhuy.coordLeak.utils;

import com.qhuy.coordLeak.CoordLeak;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class message {
    private static CoordLeak plugin = null;

    private static final Component PREFIX = MiniMessage.miniMessage().deserialize(
            CoordLeak.getInstance().getConfig().getString("prefix", "Message not found")
    );

    public message(CoordLeak plugin) {
        message.plugin = plugin;
    }

    public static Component parse(String message, Player player) {
        return MiniMessage.miniMessage().deserialize(PlaceholderAPI.setPlaceholders(player, message));
    }
    public static Component get(String placeholder) {
        return MiniMessage.miniMessage().deserialize(
                CoordLeak.getInstance().getMessage().getString(placeholder, "Message Not Found")
        );
    }
    public static void sendToSender(Component component, CommandSender sender) {
        if(sender instanceof Player player) {
            CoordLeak.getInstance().audience(sender).sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            PlaceholderAPI.setPlaceholders(
                                    player,
                                    MiniMessage.miniMessage().serialize(
                                            PREFIX.append(Component.space()).append(component)
                                    )
                            )
                    )
            );
        } else {
            CoordLeak.getInstance().audience(sender).sendMessage(
                    PREFIX.append(Component.space()).append(component)
            );
        }
    }
    public static void sendToPlayer(Component component, Player player) {
        CoordLeak.getInstance().audience(player).sendMessage(
                MiniMessage.miniMessage().deserialize(
                        PlaceholderAPI.setPlaceholders(
                                player,
                                MiniMessage.miniMessage().serialize(
                                        PREFIX.append(
                                                Component.space().append(component)
                                        )
                                )
                        )
                )
        );
    }

    private static boolean checkSender(CommandSender sender) {
        return sender instanceof Player;
    }
}
