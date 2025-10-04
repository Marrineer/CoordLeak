package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.utils.CoordLeakExpansion;
import com.qhuy.coordLeak.utils.message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class reloadCommand implements CommandExecutor {
    private final CoordLeak plugin;
    private final CoordLeakExpansion PAPI = new CoordLeakExpansion();

    public reloadCommand(CoordLeak plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, String label, String[] args) {
        if(!(sender.hasPermission("coordleak.admin"))) {
            message.sendToSender(message.get("permission"), sender);
            return true;
        }
        plugin.reloadConfig();
        plugin.reload();
        PAPI.unregister();
        PAPI.register();
        message.sendToSender(message.get("configReloaded"), sender);

        return true;
    }
}
