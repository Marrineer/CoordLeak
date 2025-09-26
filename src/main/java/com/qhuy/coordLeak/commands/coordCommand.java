package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.utils.DatabaseManager;
import com.qhuy.coordLeak.utils.message;
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
        String prefix = plugin.getConfig().getString("prefix", "");
        if(!(sender instanceof Player player)) {
            sender.sendMessage(message.get("onlyPlayer"));
            return true;
        }
        if(args.length != 0) {
            sender.sendMessage(message.parse(prefix + " " + message.get("invalidArgument"), player));
            return true;
        }
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(sender);
        if(players.isEmpty()) {
            player.sendMessage(message.parse(prefix + " " + message.get("noOneIsOnline"), player));
            return true;
        }
        databaseManager.getUsageCountAsync(player.getUniqueId(), plugin, (count) -> {
            if (count <= 0) {
                player.sendMessage(message.parse(prefix + " " + message.get("noUsageLeft"), player));
                return;
            }

            Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
            databaseManager.onUsageAsync(player.getUniqueId(), plugin);

            List<String> keys = Arrays.asList("message", "target", "coord", "dimension");
            for(String key : keys) {
                player.sendMessage(message.parse(
                        plugin.getMessage().getString("randomSelect" + key, "Message not found"),
                        player
                ));
            }
            target.sendMessage(message.get("leak.exposed"));
        });

        return true;
    }
}