package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.utils.DatabaseManager;
import com.qhuy.coordLeak.utils.message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class coordCommand implements CommandExecutor {
    private final CoordLeak plugin;
    private final DatabaseManager databaseManager;

    public coordCommand(CoordLeak plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if(!(sender instanceof Player player)) {
            message.send(message.get("onlyPlayer"), sender);
            return true;
        }
        if(args.length != 0) {
            message.send(message.get("invalidArgument"), sender);
            return true;
        }
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(sender);
        if(players.isEmpty()) {
            message.send(message.get("noOneIsOnline"), sender);
            return true;
        }
        databaseManager.getUsageCountAsync(player.getUniqueId(), plugin, (count) -> {
            if (count <= 0) {
                message.send(message.get("noUsageLeft"), sender);
                return;
            }

            Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
            databaseManager.onUsageAsync(player.getUniqueId(), plugin);

            List<String> keys = Arrays.asList("message", "target", "coord", "dimension");
            for(String key : keys) {
                message.send(message.parse(
                        plugin.getMessage().getString("randomSelect." + key, "Message not found"),
                        player
                ), sender);
            }
            message.sendToPlayer(message.get("leak.exposed"), target);
        });

        return true;
    }
}