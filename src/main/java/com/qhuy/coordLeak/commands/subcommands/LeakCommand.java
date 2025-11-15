package com.qhuy.coordLeak.commands.subcommands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.AuditLogger;
import com.qhuy.coordLeak.managers.ConfigManager;
import com.qhuy.coordLeak.managers.EconomyManager;
import com.qhuy.coordLeak.managers.ProtectionManager;
import com.qhuy.coordLeak.utils.MessageUtil;
import com.qhuy.coordLeak.utils.Permissions;
import com.qhuy.coordLeak.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class LeakCommand implements SubCommand {

    private final CoordLeak plugin;
    private final ProtectionManager protectionManager;
    private final AuditLogger auditLogger;
    private final MessageUtil messageUtil;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;

    public LeakCommand(CoordLeak plugin, ProtectionManager protectionManager, AuditLogger auditLogger) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
        this.auditLogger = auditLogger;
        this.messageUtil = plugin.getMessageUtil();
        this.configManager = plugin.getConfigManager();
        this.economyManager = plugin.getEconomyManager();
    }

    @Override
    public @NotNull String getName() {
        return "leak";
    }

    @Override
    public @NotNull String getPermission() {
        return Permissions.LEAK;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageUtil.send(sender, "player-only-command");
            return;
        }

        if (!player.hasPermission(getPermission())) {
            messageUtil.send(player, "no-permission");
            return;
        }
        
        // Validate player is still in valid state
        if (!PlayerUtil.isPlayerValid(player)) {
            messageUtil.send(player, "config-error");
            return;
        }

        String loggedIp = PlayerUtil.getLoggableIp(player, configManager.isIpLoggingEnabled());

        // Check blacklist/whitelist
        if (configManager.isBlacklistEnabled() && protectionManager.isBlacklisted(player.getUniqueId())) {
            messageUtil.send(player, "player-blacklisted");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "BLACKLISTED", loggedIp, "Player is blacklisted.");
            return;
        }
        if (configManager.isWhitelistEnabled() && !protectionManager.isWhitelisted(player.getUniqueId())) {
            messageUtil.send(player, "player-not-whitelisted");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "NOT_WHITELISTED", loggedIp, "Player is not whitelisted.");
            return;
        }

        // Check global rate limit
        if (configManager.isGlobalRateLimitEnabled() && protectionManager.isGlobalRateLimited()) {
            long remaining = protectionManager.getGlobalBlockRemaining();
            messageUtil.send(player, "global-rate-limited", "%time%", MessageUtil.formatTime(remaining));
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "GLOBAL_RATE_LIMITED", loggedIp, "Global rate limit exceeded.");
            return;
        }

        // Check cooldown
        if (protectionManager.isOnCooldown(player.getUniqueId(), "leak")) {
            long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "leak");
            messageUtil.send(player, "command-cooldown", "%time%", MessageUtil.formatTime(remaining));
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", loggedIp, "Player is on cooldown.");
            return;
        }

        // Check rate limit
        if (protectionManager.isRateLimited(player.getUniqueId(), "leak")) {
            messageUtil.send(player, "command-rate-limited");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", loggedIp, "Player is rate limited.");
            return;
        }

        // Check daily limit
        if (protectionManager.hasExceededDailyLimit(player.getUniqueId(), "leak")) {
            messageUtil.send(player, "daily-limit-exceeded");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "DAILY_LIMIT_EXCEEDED", loggedIp, "Player exceeded daily limit.");
            return;
        }

        double price = configManager.getDefaultPrice();

        // Handle economy transaction for non-admin players
        if (!protectionManager.hasBypassPermission(player.getUniqueId(), "admin")) {
            if (!economyManager.isAvailable()) {
                messageUtil.send(player, "economy-error");
                auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "ECONOMY_ERROR", loggedIp, "Economy not available.");
                return;
            }
            
            if (!economyManager.withdraw(player, price)) {
                messageUtil.send(player, "insufficient-funds", "%amount%", String.format("%.2f", price));
                auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "INSUFFICIENT_FUNDS", loggedIp, "Player has insufficient funds.");
                return;
            }
        }

        // Find valid target
        List<Player> potentialTargets = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .filter(PlayerUtil::isPlayerValid)
                .filter(protectionManager::isValidTarget)
                .collect(Collectors.toList());

        if (potentialTargets.isEmpty()) {
            messageUtil.send(player, "no-leak-target-found");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "NO_TARGET_FOUND", loggedIp, "No suitable target player found.");
            
            // Refund if payment was taken
            if (!protectionManager.hasBypassPermission(player.getUniqueId(), "admin") && economyManager.isAvailable()) {
                economyManager.refundAsync(player.getUniqueId(), price, 0);
            }
            return;
        }

        Player target = potentialTargets.get(ThreadLocalRandom.current().nextInt(potentialTargets.size()));

        // Apply limits
        protectionManager.applyCooldown(player.getUniqueId(), "leak");
        protectionManager.recordRateLimitUsage(player.getUniqueId(), "leak");
        protectionManager.incrementDailyUsage(player.getUniqueId(), "leak");

        // Send messages
        String coords = String.format("X: %d, Y: %d, Z: %d", 
            target.getLocation().getBlockX(), 
            target.getLocation().getBlockY(), 
            target.getLocation().getBlockZ());
            
        messageUtil.sendList(player, "leak-success", 
            "%player%", target.getName(), 
            "%coordleak_posx%", String.valueOf(target.getLocation().getBlockX()), 
            "%coordleak_posz%", String.valueOf(target.getLocation().getBlockZ()), 
            "%coordleak_dimension%", target.getWorld().getName());
            
        messageUtil.send(target, "leak-exposed");

        auditLogger.log("LEAK", player.getName(), target.getName(), price, "SUCCESS", loggedIp, 
            "Leaked " + target.getName() + "'s coordinates: " + coords);
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
