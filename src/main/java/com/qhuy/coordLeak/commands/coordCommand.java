package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.utils.DatabaseManager;
import com.qhuy.coordLeak.utils.message;
import me.clip.placeholderapi.PlaceholderAPI;
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
            message.sendToSender(message.get("onlyPlayer"), sender);
            return true;
        }
        if(args.length != 0) {
            message.sendToSender(message.get("helpFallback.coordusage"), sender);
            return true;
        }
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(player);
        if(players.isEmpty()) {
            message.sendToSender(message.get("noOneIsOnline"), sender);
            return true;
        }
        databaseManager.getUsageCountAsync(player.getUniqueId(), plugin, (count) -> {
            if (count <= 0) {
                message.sendToSender(message.get("noUsageLeft"), sender);
                return;
            }

            Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
            if (!player.isOp() && !player.hasPermission("coordleak.admin")) {
                databaseManager.onUsageAsync(player.getUniqueId(), plugin);
            }
            List<String> keys = Arrays.asList("message", "target", "coord", "dimension");
            for(String key : keys) {
                CoordLeak.getInstance().audience(player).sendMessage(
                        MiniMessage.miniMessage().deserialize(
                                PlaceholderAPI.setPlaceholders(
                                        target,
                                        MiniMessage.miniMessage().serialize(
                                                message.get("randomSelect." + key)
                                        )
                                )
                        )
                );
            }
            message.sendToPlayer(message.get("leak.exposed"), target);
        });

        return true;
    }
}