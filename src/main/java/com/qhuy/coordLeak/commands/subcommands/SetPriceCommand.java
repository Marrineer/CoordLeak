package com.qhuy.coordLeak.commands.subcommands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.AuditLogger;
import com.qhuy.coordLeak.managers.ConfigManager;
import com.qhuy.coordLeak.managers.ProtectionManager;
import com.qhuy.coordLeak.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

public class SetPriceCommand implements SubCommand {

    private final CoordLeak plugin;
    private final ProtectionManager protectionManager;
    private final AuditLogger auditLogger;
    private final MessageUtil messageUtil;
    private final ConfigManager configManager;

    public SetPriceCommand(CoordLeak plugin, ProtectionManager protectionManager, AuditLogger auditLogger) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
        this.auditLogger = auditLogger;
        this.messageUtil = plugin.getMessageUtil();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public @NotNull String getName() {
        return "setprice";
    }

    @Override
    public @NotNull String getPermission() {
        return "coordleak.setprice";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(getPermission())) {
            messageUtil.send(sender, "no-permission");
            return;
        }

        String ip = "N/A";
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
            InetSocketAddress socketAddr = player.getAddress();
            if (socketAddr != null && socketAddr.getAddress() != null) {
                ip = socketAddr.getAddress().getHostAddress();
            } else {
                ip = "unknown";
            }
        }

        String loggedIp;
        if (!configManager.isIpLoggingEnabled()) {
            loggedIp = "REDACTED";
        } else {
            loggedIp = ip;
        }

        if (player != null) { // Only apply cooldown/ratelimit if sender is a player
            if (protectionManager.isOnCooldown(player.getUniqueId(), "setprice")) {
                long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "setprice");
                messageUtil.send(player, "command-cooldown", "%time%", MessageUtil.formatTime(remaining));
                auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", loggedIp, "Player is on cooldown for setprice.");
                return;
            }
            if (protectionManager.isRateLimited(player.getUniqueId(), "setprice")) {
                messageUtil.send(player, "command-rate-limited");
                auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", loggedIp, "Player is rate limited for setprice.");
                return;
            }
        }

        if (args.length < 1) {
            double currentPrice = configManager.getDefaultPrice();
            messageUtil.send(sender, "current-price", "%price%", String.format("%.2f", currentPrice));
            return;
        }

        double newPrice;
        try {
            newPrice = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            messageUtil.send(sender, "invalid-price");
            auditLogger.log("SET_PRICE_ATTEMPT", sender.getName(), args[0], 0, "INVALID_NUMBER_FORMAT", loggedIp, "Invalid number format for price.");
            return;
        }

        if (newPrice < configManager.getMinPrice() || newPrice > configManager.getMaxPrice()) {
            messageUtil.send(sender, "price-out-of-range",
                    "%min_price%", String.format("%.2f", configManager.getMinPrice()),
                    "%max_price%", String.format("%.2f", configManager.getMaxPrice()));
            auditLogger.log("SET_PRICE_ATTEMPT", sender.getName(), String.valueOf(newPrice), newPrice, "PRICE_OUT_OF_RANGE", loggedIp, "Price out of configured range.");
            return;
        }

        if (player != null) {
            boolean confirmRequired = configManager.isReloadRequireConfirm();
            boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");

            if (confirmRequired && !confirmed) {
                String commandToConfirm = "setprice " + args[0];
                String time = String.valueOf(configManager.getConfirmationTimeout("setprice") / 1000);
                protectionManager.initiateConfirmation(player.getUniqueId(), "setprice");
                messageUtil.send(player, "reload-confirm-required", "%command%", commandToConfirm, "%time%", time);
                auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), String.valueOf(newPrice), newPrice, "CONFIRM_PENDING", loggedIp, "Player initiated setprice confirmation.");
                return;
            }

            if (confirmRequired) { // confirmed is true
                if (!protectionManager.hasPendingConfirmation(player.getUniqueId(), "setprice")) {
                    messageUtil.send(player, "reload-cancelled");
                    auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), String.valueOf(newPrice), newPrice, "CONFIRM_EXPIRED", loggedIp, "Player tried to confirm setprice but no pending confirmation.");
                    return;
                }
                protectionManager.clearConfirmation(player.getUniqueId(), "setprice");
            }
        }

        configManager.setDefaultPrice(newPrice);
        if (player != null) {
            protectionManager.applyCooldown(player.getUniqueId(), "setprice");
            protectionManager.recordRateLimitUsage(player.getUniqueId(), "setprice");
        }
        messageUtil.send(sender, "setprice-success", "%price%", String.format("%.2f", newPrice));
        auditLogger.log("SET_PRICE", sender.getName(), "N/A", newPrice, "SUCCESS", loggedIp, "Price set to " + newPrice);
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2 && sender.hasPermission(getPermission())) {
            if ("confirm".startsWith(args[1].toLowerCase())) {
                return Collections.singletonList("confirm");
            }
        }
        return Collections.emptyList();
    }
}
