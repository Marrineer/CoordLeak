package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.DatabaseManager;
import com.qhuy.coordLeak.utils.CoordLeakExpansion;
import com.qhuy.coordLeak.utils.message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class reloadCommand implements CommandExecutor {
    private final CoordLeak plugin;
    private final DatabaseManager databaseManager;
    private final CoordLeakExpansion PAPI;

    public reloadCommand(CoordLeak plugin, DatabaseManager databaseManager, CoordLeakExpansion PAPI) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.PAPI = PAPI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, String label, String[] args) {
        if(!(sender.hasPermission("coordleak.admin"))) {
            message.sendToSender(message.get("permission"), sender);
            return true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Config & message
            plugin.reloadConfig();
            plugin.getMessageManager().reloadMessage();

            // PlaceholderAPI
            PAPI.unregister();
            PAPI.register();

            // Database
            databaseManager.disconnect();
            try {
                databaseManager.connect();
            } catch (SQLException e) {
                e.fillInStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getPluginManager().disablePlugin(plugin);
                });
            }
        });
        message.sendToSender(message.get("configReloaded"), sender);

        return true;
    }
}
