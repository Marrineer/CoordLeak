package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.utils.message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class reloadCommand implements CommandExecutor {
    private final CoordLeak plugin;

    public reloadCommand(CoordLeak plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, String label, String[] args) {
        if(!(sender.hasPermission("coordleak.admin"))) {
            sender.sendMessage(message.get("permission"));
            return true;
        }
        plugin.reloadConfig();
        plugin.reload();
        sender.sendMessage(message.get("configReloaded"));

        return true;
    }
}
