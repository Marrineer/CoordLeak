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
        String prefix = plugin.getConfig().getString("prefix");
        if(!(sender.hasPermission("coordleak.admin"))) {
            sender.sendMessage(message.get("permission"));
            return true;
        }
        if(args.length != 2) {
            sender.sendMessage(message.get("invalidArgument"));
            return true;
        }
        int count;
        try {
            count = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(prefix + " Invalid Number"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if(target == null) {
            sender.sendMessage(message.get("invalidPlayer"));
            return true;
        }
        databaseManager.setUsageCountAsync(target.getUniqueId(), plugin, count);
        sender.sendMessage(message.get("setSuccess"));

        return true;
    }
}