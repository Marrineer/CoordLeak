package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SetPriceCommand implements CommandExecutor {

    private final CoordLeak plugin;
    private final MessageUtil messageUtil;

    public SetPriceCommand(CoordLeak plugin) {
        this.plugin = plugin;
        this.messageUtil = plugin.getMessageUtil();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("coordleak.admin")) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("permission", "No permission!"), sender);
            return true;
        }

        if (args.length == 0) {
            double currentPrice = plugin.getConfigManager().getPrice();
            messageUtil.sendToSender(plugin.getMessageManager().getString("currentPrice", "The current price is: %price%").replace("%price%", String.valueOf(currentPrice)), sender);
            return true;
        }

        try {
            double newPrice = Double.parseDouble(args[0]);
            if (newPrice < 0) {
                messageUtil.sendToSender(plugin.getMessageManager().getString("priceNegative", "Price cannot be negative."), sender);
                return true;
            }
            plugin.getConfigManager().setPrice(newPrice);
            messageUtil.sendToSender(plugin.getMessageManager().getString("priceSet", "Price has been set to: %price%").replace("%price%", String.valueOf(newPrice)), sender);
        } catch (NumberFormatException e) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("invalidNumber", "Invalid number format."), sender);
        }

        return true;
    }
}
