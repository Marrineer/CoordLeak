package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.DatabaseManager;
import com.qhuy.coordLeak.utils.message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class buyusageCommand implements CommandExecutor {
    private final CoordLeak plugin;
    private final DatabaseManager databaseManager;

    public buyusageCommand(CoordLeak plugin, DatabaseManager databaseManager) {
        this.plugin = plugin; 
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        double price = plugin.getConfig().getDouble("price", 500);
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            message.sendToSender(message.get("onlyPlayer"), sender);
            return true;
        }
        if (args.length != 0) {
            message.sendToSender(message.get("helpFallback.buyusage"), sender);
            return true;
        }
        UUID targetUUID = player.getUniqueId();
        double balance = plugin.getEconomy().getBalance(player);
        if (balance < price) {
            message.sendToSender(message.get("soPoor"), sender);
            return true;
        }
        plugin.getEconomy().withdrawPlayer(player, price);
        databaseManager.addUsageCountAsync(targetUUID, plugin);
        message.sendToSender(message.get("buySuccessfully"), sender);

        return true;
    }
}