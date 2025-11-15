package com.qhuy.coordLeak.commands.subcommands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.AuditLogger;
import com.qhuy.coordLeak.managers.ConfigManager;
import com.qhuy.coordLeak.managers.ProtectionManager;
import com.qhuy.coordLeak.utils.MessageUtil;
import com.qhuy.coordLeak.utils.Sanitizer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ShareCommand implements SubCommand {

    private final CoordLeak plugin;
    private final ProtectionManager protectionManager;
    private final AuditLogger auditLogger;
    private final MessageUtil messageUtil;
    private final ConfigManager configManager;
    private final Sanitizer sanitizer;

    public ShareCommand(CoordLeak plugin, ProtectionManager protectionManager, AuditLogger auditLogger) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
        this.auditLogger = auditLogger;
        this.messageUtil = plugin.getMessageUtil();
        this.configManager = plugin.getConfigManager();
        this.sanitizer = plugin.getSanitizer();
    }

    @Override
    public @NotNull String getName() {
        return "share";
    }

    @Override
    public @NotNull String getPermission() {
        return "coordleak.share";
    }

    private String resolvePlayerIp(Player player) {
        InetSocketAddress socketAddr = player.getAddress();
        if (socketAddr == null || socketAddr.getAddress() == null) return "N/A";
        return socketAddr.getAddress().getHostAddress();
    }

    private String getLoggedIp(Player player) {
        if (!configManager.isIpLoggingEnabled()) {
            return "REDACTED";
        }
        return resolvePlayerIp(player);
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

        String loggedIp = getLoggedIp(player);

        if (configManager.isBlacklistEnabled() && protectionManager.isBlacklisted(player.getUniqueId())) {
            messageUtil.send(player, "player-blacklisted");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "BLACKLISTED", loggedIp, "Player is blacklisted.");
            return;
        }
        if (configManager.isWhitelistEnabled() && !protectionManager.isWhitelisted(player.getUniqueId())) {
            messageUtil.send(player, "player-not-whitelisted");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "NOT_WHITELISTED", loggedIp, "Player is not whitelisted.");
            return;
        }

        if (protectionManager.isOnCooldown(player.getUniqueId(), "share")) {
            long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "share");
            messageUtil.send(player, "command-cooldown", "%time%", MessageUtil.formatTime(remaining));
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", loggedIp, "Player is on cooldown.");
            return;
        }

        if (protectionManager.isRateLimited(player.getUniqueId(), "share")) {
            messageUtil.send(player, "command-rate-limited");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", loggedIp, "Player is rate limited.");
            return;
        }

        if (protectionManager.hasExceededDailyLimit(player.getUniqueId(), "share")) {
            messageUtil.send(player, "daily-limit-exceeded");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "DAILY_LIMIT_EXCEEDED", loggedIp, "Player exceeded daily limit.");
            return;
        }

        if (args.length < 1) {
            messageUtil.sendList(sender, "share-usage");
            return;
        }
        Player targetShare = Bukkit.getPlayer(args[0]);
        if (targetShare == null) {
            messageUtil.send(player, "target-not-online");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), args[0], 0, "TARGET_NOT_ONLINE", loggedIp, "Target player not found or offline.");
            return;
        }
        if (player.getUniqueId().equals(targetShare.getUniqueId())) {
            messageUtil.send(player, "cannot-target-yourself");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "TARGET_SELF", loggedIp, "Player tried to share with self.");
            return;
        }

        if (!protectionManager.isValidTarget(targetShare)) {
            if (configManager.getExcludedWorlds().contains(targetShare.getWorld().getName())) {
                messageUtil.send(player, "target-excluded-world");
                auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "TARGET_EXCLUDED_WORLD", loggedIp, "Target in excluded world.");
            } else {
                messageUtil.send(player, "target-excluded-permission");
                auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "TARGET_EXCLUDED_PERMISSION", loggedIp, "Target has excluded permission.");
            }
            return;
        }

        if (configManager.isConsentRequiredForShare() && !protectionManager.hasTargetConsent(targetShare.getUniqueId())) {
            messageUtil.send(player, "target-no-consent");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "NO_CONSENT", loggedIp, "Target has not consented to share.");
            return;
        }

        protectionManager.applyCooldown(player.getUniqueId(), "share");
        protectionManager.recordRateLimitUsage(player.getUniqueId(), "share");
        protectionManager.incrementDailyUsage(player.getUniqueId(), "share");

        String customText = "";
        if (args.length > 1) {
            String rawText = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            if (!player.hasPermission("coordleak.admin")) {
                customText = " " + sanitizer.sanitize(rawText);
            } else {
                customText = " " + rawText;
            }
        }

        String coords = String.format("X: %d, Y: %d, Z: %d", player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());

        messageUtil.send(player, "share-success-sender", "%target%", targetShare.getName());
        messageUtil.send(targetShare, "share-success-target", "%sender%", player.getName(), "%coords%", coords + customText);

        auditLogger.log("SHARE", player.getName(), targetShare.getName(), 0, "SUCCESS", loggedIp, "Shared " + player.getName() + "'s coordinates with " + targetShare.getName() + ": " + coords + (!customText.isEmpty() ? " (Custom:" + customText + ")" : ""));
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission(getPermission())) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}