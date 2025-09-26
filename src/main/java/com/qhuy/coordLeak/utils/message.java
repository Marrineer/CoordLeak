package com.qhuy.coordLeak.utils;

import com.qhuy.coordLeak.CoordLeak;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class message {
    private static CoordLeak plugin = null;

    public message(CoordLeak plugin) {
        message.plugin = plugin;
    }

    public static Component parse(String message, Player player) {
        return MiniMessage.miniMessage().deserialize(PlaceholderAPI.setPlaceholders(player, message));
    }
    public static Component get(String placeholder) {
        return MiniMessage.miniMessage().deserialize(CoordLeak.getInstance().getMessage().getString(placeholder));
    }
}
