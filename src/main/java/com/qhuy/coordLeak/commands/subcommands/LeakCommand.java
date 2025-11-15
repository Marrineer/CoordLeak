package com.qhuy.coordLeak.commands.subcommands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.AuditLogger;
import com.qhuy.coordLeak.managers.ConfigManager;
import com.qhuy.coordLeak.managers.MessageManager;
import com.qhuy.coordLeak.managers.ProtectionManager;
import com.qhuy.coordLeak.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class LeakCommand implements SubCommand {

    private final CoordLeak plugin;
    private final ProtectionManager protectionManager;
    private final AuditLogger auditLogger;
    private final MessageUtil messageUtil;
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    private static final ConcurrentHashMap<UUID, ReentrantLock> LOCKS = new ConcurrentHashMap<>();


    public LeakCommand(CoordLeak plugin, ProtectionManager protectionManager, AuditLogger auditLogger) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
        this.auditLogger = auditLogger;
        this.messageUtil = plugin.getMessageUtil();
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public @NotNull String getName() {
        return "leak";
    }

    @Override
    public @NotNull String getPermission() {
        return "coordleak.leak";
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

        // Fix 2: Extract player IP once, avoid chained NPE
        String ip;
        InetSocketAddress socketAddr = player.getAddress();
        if (socketAddr != null && socketAddr.getAddress() != null) {
            ip = socketAddr.getAddress().getHostAddress();
        } else {
            ip = "unknown";
        }

        // Fix 2: Replace ALL occurrences of IP retrieval with this variable.
        // Fix 2: Also apply privacy setting for logging IP.
        String loggedIp;
        if (!configManager.isIpLoggingEnabled()) {
            loggedIp = "REDACTED";
        } else {
            loggedIp = ip;
        }


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

        if (configManager.isGlobalRateLimitEnabled() && protectionManager.isGlobalRateLimited()) {
            long remaining = protectionManager.getGlobalBlockRemaining();
            messageUtil.send(player, "global-rate-limited", "%time%", MessageUtil.formatTime(remaining));
            String logMessage = messageManager.getString("global-limit-exceeded-log", "[CoordLeak] Global rate limit exceeded. Blocking requests for %time% seconds.");
            plugin.getLogger().warning(logMessage.replace("%time%", String.valueOf(configManager.getGlobalRateLimitBlockDuration() / 1000)));
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "GLOBAL_RATE_LIMITED", loggedIp, "Global rate limit exceeded.");
            return;
        }

        if (protectionManager.isOnCooldown(player.getUniqueId(), "leak")) {
            long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "leak");
            messageUtil.send(player, "command-cooldown", "%time%", MessageUtil.formatTime(remaining));
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", loggedIp, "Player is on cooldown.");
            return;
        }

        if (protectionManager.isRateLimited(player.getUniqueId(), "leak")) {
            messageUtil.send(player, "command-rate-limited");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", loggedIp, "Player is rate limited.");
            return;
        }

        if (protectionManager.hasExceededDailyLimit(player.getUniqueId(), "leak")) {
            messageUtil.send(player, "daily-limit-exceeded");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "DAILY_LIMIT_EXCEEDED", loggedIp, "Player exceeded daily limit.");
            return;
        }

        double price = configManager.getDefaultPrice();

        if (!protectionManager.hasBypassPermission(player.getUniqueId(), "admin")) {
            if (plugin.getEconomy() == null) {
                messageUtil.send(player, "economy-error");
                auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "ECONOMY_ERROR", loggedIp, "Economy not available.");
                return;
            }
            if (plugin.getEconomy().getBalance(player) < price) {
                messageUtil.send(player, "insufficient-funds", "%amount%", String.format("%.2f", price));
                auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "INSUFFICIENT_FUNDS", loggedIp, "Player has insufficient funds.");
                return;
            }

            // Fix 3: Remove String.intern() locking, replace with proper lock registry
            UUID uuid = player.getUniqueId();
            ReentrantLock lock = LOCKS.computeIfAbsent(uuid, id -> new ReentrantLock());
            lock.lock();
            try {
                if (plugin.getEconomy().getBalance(player) < price) {
                    messageUtil.send(player, "insufficient-funds", "%amount%", String.format("%.2f", price));
                    auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "INSUFFICIENT_FUNDS_CONCURRENT", loggedIp, "Player has insufficient funds due to concurrent access.");
                    return;
                }

                if (!plugin.getEconomy().withdrawPlayer(player, price).transactionSuccess()) {
                    messageUtil.send(player, "economy-error");
                    auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "WITHDRAW_FAILED", loggedIp, "Economy withdrawal failed.");
                    return;
                }
            } finally {
                lock.unlock();
                LOCKS.remove(uuid, lock);
            }
        }

        List<Player> potentialTargets = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .filter(protectionManager::isValidTarget)
                .collect(Collectors.toList());

        if (potentialTargets.isEmpty()) {
            messageUtil.send(player, "no-leak-target-found");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "NO_TARGET_FOUND", loggedIp, "No suitable target player found.");
            if (!protectionManager.hasBypassPermission(player.getUniqueId(), "admin") && plugin.getEconomy() != null) {
                plugin.getEconomy().depositPlayer(player, price);
                auditLogger.log("LEAK_REFUND", player.getName(), "N/A", price, "SUCCESS", loggedIp, "Refunded due to no target found.");
            }
            return;
        }

        Player target = potentialTargets.get(ThreadLocalRandom.current().nextInt(potentialTargets.size()));

        protectionManager.applyCooldown(player.getUniqueId(), "leak");
        protectionManager.recordRateLimitUsage(player.getUniqueId(), "leak");
        protectionManager.incrementDailyUsage(player.getUniqueId(), "leak");

        String coords = String.format("X: %d, Y: %d, Z: %d", target.getLocation().getBlockX(), target.getLocation().getBlockY(), target.getLocation().getBlockZ());
        messageUtil.sendList(player, "leak-success", "%player%", target.getName(), "%coordleak_posx%", String.valueOf(target.getLocation().getBlockX()), "%coordleak_posz%", String.valueOf(target.getLocation().getBlockZ()), "%coordleak_dimension%", target.getWorld().getName());
        messageUtil.send(target, "leak-exposed");

        auditLogger.log("LEAK", player.getName(), target.getName(), price, "SUCCESS", loggedIp, "Leaked " + target.getName() + "'s coordinates: " + coords);
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
