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

public class setusageCommand implements CommandExecutor {
    private final CoordLeak plugin;
    private final DatabaseManager databaseManager;

    public setusageCommand(CoordLeak plugin, DatabaseManager databaseManager) {
        this.plugin = plugin; 
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if(!(sender.hasPermission("coordleak.admin"))) {
            message.sendToSender(message.get("permission"), sender);
            return true;
        }
        if(args.length != 2) {
            message.sendToSender(message.get("helpFallback.setusage"), sender);
            return true;
        }
        int count;
        try {
            count = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            message.sendToSender(message.get("helpFallback.setusage"), sender);
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if(target == null) {
            message.sendToSender(message.get("invalidPlayer"), sender);
            return true;
        }
        databaseManager.setUsageCountAsync(target.getUniqueId(), plugin, count);
        message.sendToSender(message.get("setSuccess"), sender);

        return true;
    }
}