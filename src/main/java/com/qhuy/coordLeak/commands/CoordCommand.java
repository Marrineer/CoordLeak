package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.AuditLogger;
import com.qhuy.coordLeak.managers.ProtectionManager;
import com.qhuy.coordLeak.utils.MessageUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CoordCommand implements CommandExecutor, TabCompleter {
    private final CoordLeak plugin;
    private final ProtectionManager protectionManager;
    private final AuditLogger auditLogger;
    private final MessageUtil messageUtil;

    public CoordCommand(CoordLeak plugin, ProtectionManager protectionManager, AuditLogger auditLogger) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
        this.auditLogger = auditLogger;
        this.messageUtil = plugin.getMessageUtil();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) {
            handleHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender, args);
                break;
            case "leak":
                handleLeak(sender);
                break;
            case "share":
                handleShare(sender, args);
                break;
            case "info":
                handleInfo(sender);
                break;
            case "setprice": // Handle setprice as a subcommand
                handleSetPrice(sender, args);
                break;
            default:
                handleHelp(sender);
                break;
        }
        return true;
    }

    private void handleHelp(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        List<String> strList = plugin.getMessageManager().getStringList("help", Collections.emptyList());
        if (strList.isEmpty()) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("configError", "Config error!"), sender);
            return;
        }
        for (String msg : strList) {
            String processedMsg = plugin.hasPAPI() && player != null ? PlaceholderAPI.setPlaceholders(player, msg) : msg;
            plugin.audience(sender).sendMessage(MiniMessage.miniMessage().deserialize(processedMsg));
        }
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            if (!sender.hasPermission("coordleak.admin")) {
                messageUtil.sendToSender(plugin.getMessageManager().getNoPermission(), sender);
                return;
            }
            // Console can reload directly
            plugin.reloadManagers();
            messageUtil.sendToSender(plugin.getMessageManager().getReloadConfirmed(), sender);
            auditLogger.log("RELOAD", sender.getName(), "N/A", 0, "SUCCESS", "N/A", "Plugin reloaded by console.");
            return;
        }

        if (!player.hasPermission("coordleak.admin")) {
            messageUtil.sendToSender(plugin.getMessageManager().getNoPermission(), player);
            return;
        }

        boolean confirmRequired = plugin.getConfigManager().isReloadRequireConfirm();
        boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");

        if (confirmRequired && !confirmed) {
            if (protectionManager.hasPendingConfirmation(player.getUniqueId(), "reload")) {
                messageUtil.sendToSender(plugin.getMessageManager().getReloadConfirmRequired().replace("%command%", "reload").replace("%time%", String.valueOf(plugin.getConfigManager().getConfirmationTimeout("reload") / 1000)), player);
            } else {
                protectionManager.initiateConfirmation(player.getUniqueId(), "reload");
                messageUtil.sendToSender(plugin.getMessageManager().getReloadConfirmRequired().replace("%command%", "reload").replace("%time%", String.valueOf(plugin.getConfigManager().getConfirmationTimeout("reload") / 1000)), player);
            }
            auditLogger.log("RELOAD_ATTEMPT", player.getName(), "N/A", 0, "CONFIRM_PENDING", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player initiated reload confirmation.");
            return;
        }

        if (confirmRequired && confirmed) {
            if (!protectionManager.hasPendingConfirmation(player.getUniqueId(), "reload")) {
                messageUtil.sendToSender(plugin.getMessageManager().getReloadCancelled(), player); // Confirmation expired or not initiated
                auditLogger.log("RELOAD_ATTEMPT", player.getName(), "N/A", 0, "CONFIRM_EXPIRED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player tried to confirm reload but no pending confirmation.");
                return;
            }
            protectionManager.clearConfirmation(player.getUniqueId(), "reload");
        }

        // If we reach here, reload is confirmed or not required
        plugin.reloadManagers();
        // Removed cooldown and rate limit application for reload command
        messageUtil.sendToSender(plugin.getMessageManager().getReloadConfirmed(), player);
        auditLogger.log("RELOAD", player.getName(), "N/A", 0, "SUCCESS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Plugin reloaded by player.");
    }

    private void handleSetPrice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coordleak.setprice")) {
            messageUtil.sendToSender(plugin.getMessageManager().getNoPermission(), sender);
            return;
        }

        // Cooldown for setprice command
        if (sender instanceof Player player && protectionManager.isOnCooldown(player.getUniqueId(), "setprice")) {
            long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "setprice");
            messageUtil.sendToSender(plugin.getMessageManager().getCommandCooldown().replace("%time%", formatTime(remaining)), player);
            auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is on cooldown for setprice.");
            return;
        }

        // Rate limit for setprice command
        if (sender instanceof Player player && protectionManager.isRateLimited(player.getUniqueId(), "setprice")) {
            messageUtil.sendToSender(plugin.getMessageManager().getCommandRateLimited(), player);
            auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is rate limited for setprice.");
            return;
        }

        if (args.length < 2) { // Expecting /coord setprice <amount>
            double currentPrice = plugin.getConfigManager().getDefaultPrice();
            messageUtil.sendToSender(plugin.getMessageManager().getString("currentPrice", "&aThe current price for leaking coordinates is: &e%price%&a.").replace("%price%", String.format("%.2f", currentPrice)), sender);
            return;
        }

        double newPrice;
        try {
            newPrice = Double.parseDouble(args[1]); // args[1] is the amount
        } catch (NumberFormatException e) {
            messageUtil.sendToSender(plugin.getMessageManager().getInvalidPrice(), sender);
            auditLogger.log("SET_PRICE_ATTEMPT", sender.getName(), args[1], 0, "INVALID_NUMBER_FORMAT", sender instanceof Player ? ((Player) sender).getAddress().getAddress().getHostAddress() : "N/A", "Invalid number format for price.");
            return;
        }

        if (newPrice < plugin.getConfigManager().getMinPrice() || newPrice > plugin.getConfigManager().getMaxPrice()) {
            messageUtil.sendToSender(plugin.getMessageManager().getPriceOutOfRange()
                    .replace("%min_price%", String.format("%.2f", plugin.getConfigManager().getMinPrice()))
                    .replace("%max_price%", String.format("%.2f", plugin.getConfigManager().getMaxPrice())), sender);
            auditLogger.log("SET_PRICE_ATTEMPT", sender.getName(), String.valueOf(newPrice), newPrice, "PRICE_OUT_OF_RANGE", sender instanceof Player ? ((Player) sender).getAddress().getAddress().getHostAddress() : "N/A", "Price out of configured range.");
            return;
        }

        // Confirmation logic for players
        if (sender instanceof Player player) {
            boolean confirmRequired = plugin.getConfigManager().isReloadRequireConfirm(); // Re-using this config for setprice
            boolean confirmed = args.length > 2 && args[2].equalsIgnoreCase("confirm"); // args[2] is 'confirm'

            if (confirmRequired && !confirmed) {
                if (protectionManager.hasPendingConfirmation(player.getUniqueId(), "setprice")) {
                    messageUtil.sendToSender(plugin.getMessageManager().getReloadConfirmRequired().replace("%command%", "setprice " + args[1]).replace("%time%", String.valueOf(plugin.getConfigManager().getConfirmationTimeout("setprice") / 1000)), player);
                } else {
                    protectionManager.initiateConfirmation(player.getUniqueId(), "setprice");
                    messageUtil.sendToSender(plugin.getMessageManager().getReloadConfirmRequired().replace("%command%", "setprice " + args[1]).replace("%time%", String.valueOf(plugin.getConfigManager().getConfirmationTimeout("setprice") / 1000)), player);
                }
                auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), String.valueOf(newPrice), newPrice, "CONFIRM_PENDING", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player initiated setprice confirmation.");
                return;
            }

            if (confirmRequired && confirmed) {
                if (!protectionManager.hasPendingConfirmation(player.getUniqueId(), "setprice")) {
                    messageUtil.sendToSender(plugin.getMessageManager().getReloadCancelled(), player); // Confirmation expired or not initiated
                    auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), String.valueOf(newPrice), newPrice, "CONFIRM_EXPIRED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player tried to confirm setprice but no pending confirmation.");
                    return;
                }
                protectionManager.clearConfirmation(player.getUniqueId(), "setprice");
            }
        }

        // If we reach here, price is valid and confirmed (if required)
        plugin.getConfigManager().setDefaultPrice(newPrice);
        if (sender instanceof Player player) {
            protectionManager.applyCooldown(player.getUniqueId(), "setprice");
            protectionManager.recordRateLimitUsage(player.getUniqueId(), "setprice");
        }
        messageUtil.sendToSender(plugin.getMessageManager().getSetPriceSuccess().replace("%price%", String.format("%.2f", newPrice)), sender);
        auditLogger.log("SET_PRICE", sender.getName(), "N/A", newPrice, "SUCCESS", sender instanceof Player ? ((Player) sender).getAddress().getAddress().getHostAddress() : "N/A", "Price set to " + newPrice);
    }

    private void handleLeak(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageUtil.sendToSender(plugin.getMessageManager().getPlayerOnlyCommand(), sender);
            return;
        }

        if (!player.hasPermission("coordleak.leak")) {
            messageUtil.sendToSender(plugin.getMessageManager().getNoPermission(), player);
            return;
        }

        // Blacklist/Whitelist check
        if (plugin.getConfigManager().isBlacklistEnabled() && protectionManager.isBlacklisted(player.getUniqueId())) {
            messageUtil.sendToSender(plugin.getMessageManager().getPlayerBlacklisted(), player);
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "BLACKLISTED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is blacklisted.");
            return;
        }
        if (plugin.getConfigManager().isWhitelistEnabled() && !protectionManager.isWhitelisted(player.getUniqueId())) {
            messageUtil.sendToSender(plugin.getMessageManager().getPlayerNotWhitelisted(), player);
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "NOT_WHITELISTED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is not whitelisted.");
            return;
        }

        // Global Rate Limit check
        if (plugin.getConfigManager().isGlobalRateLimitEnabled() && protectionManager.isGlobalRateLimited()) {
            long remaining = protectionManager.getGlobalBlockRemaining();
            messageUtil.sendToSender(plugin.getMessageManager().getGlobalRateLimited().replace("%time%", formatTime(remaining)), player);
            plugin.getLogger().warning(plugin.getMessageManager().getGlobalLimitExceededLog().replace("%time%", String.valueOf(plugin.getConfigManager().getGlobalRateLimitBlockDuration() / 1000)));
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "GLOBAL_RATE_LIMITED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Global rate limit exceeded.");
            return;
        }

        // Cooldown check
        if (protectionManager.isOnCooldown(player.getUniqueId(), "leak")) {
            long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "leak");
            messageUtil.sendToSender(plugin.getMessageManager().getCommandCooldown().replace("%time%", formatTime(remaining)), player);
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is on cooldown.");
            return;
        }

        // Rate Limit check
        if (protectionManager.isRateLimited(player.getUniqueId(), "leak")) {
            messageUtil.sendToSender(plugin.getMessageManager().getCommandRateLimited(), player);
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is rate limited.");
            return;
        }

        // Daily Limit check
        if (protectionManager.hasExceededDailyLimit(player.getUniqueId(), "leak")) {
            messageUtil.sendToSender(plugin.getMessageManager().getDailyLimitExceeded(), player);
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "DAILY_LIMIT_EXCEEDED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player exceeded daily limit.");
            return;
        }

        double price = plugin.getConfigManager().getDefaultPrice();

        // Admin bypass for economy
        if (!protectionManager.hasBypassPermission(player.getUniqueId(), "admin")) {
            // Economy check
            if (plugin.getEconomy() == null) {
                messageUtil.sendToSender(plugin.getMessageManager().getEconomyError(), player);
                auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "ECONOMY_ERROR", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Economy not available.");
                return;
            }
            if (plugin.getEconomy().getBalance(player) < price) {
                messageUtil.sendToSender(plugin.getMessageManager().getInsufficientFunds().replace("%amount%", String.format("%.2f", price)), player);
                auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "INSUFFICIENT_FUNDS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player has insufficient funds.");
                return;
            }

            // Transaction safety: withdraw before proceeding
            // Synchronize on player UUID to prevent double-withdrawal from spamming
            synchronized (player.getUniqueId().toString().intern()) {
                // Re-check balance inside synchronized block
                if (plugin.getEconomy().getBalance(player) < price) {
                    messageUtil.sendToSender(plugin.getMessageManager().getInsufficientFunds().replace("%amount%", String.format("%.2f", price)), player);
                    auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "INSUFFICIENT_FUNDS_CONCURRENT", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player has insufficient funds due to concurrent access.");
                    return;
                }

                EconomyResponse r = plugin.getEconomy().withdrawPlayer(player, price);
                if (!r.transactionSuccess()) {
                    messageUtil.sendToSender(plugin.getMessageManager().getEconomyError(), player);
                    plugin.getLogger().severe("Economy withdrawal failed for " + player.getName() + ": " + r.errorMessage);
                    auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "WITHDRAW_FAILED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Economy withdrawal failed: " + r.errorMessage);
                    return;
                }
            }
        }

        // Find a target
        List<Player> potentialTargets = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId())) // Cannot leak self
                .filter(protectionManager::isValidTarget) // Check excluded worlds/permissions
                .collect(Collectors.toList());

        if (potentialTargets.isEmpty()) {
            messageUtil.sendToSender(plugin.getMessageManager().getNoLeakTargetFound(), player);
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "NO_TARGET_FOUND", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "No suitable target player found.");
            // If economy was involved, refund the player if no target was found
            if (!protectionManager.hasBypassPermission(player.getUniqueId(), "admin") && plugin.getEconomy() != null) {
                plugin.getEconomy().depositPlayer(player, price);
                auditLogger.log("LEAK_REFUND", player.getName(), "N/A", price, "SUCCESS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Refunded due to no target found.");
            }
            return;
        }

        Player target = potentialTargets.get(ThreadLocalRandom.current().nextInt(potentialTargets.size()));

        // Record usage after successful transaction and target selection
        protectionManager.applyCooldown(player.getUniqueId(), "leak");
        protectionManager.recordRateLimitUsage(player.getUniqueId(), "leak");
        protectionManager.incrementDailyUsage(player.getUniqueId(), "leak");

        String coords = String.format("X: %d, Y: %d, Z: %d", target.getLocation().getBlockX(), target.getLocation().getBlockY(), target.getLocation().getBlockZ());

        String leakMessage = plugin.getMessageManager().getLeakSuccess()
                .replace("%player%", target.getName())
                .replace("%coords%", coords);

        messageUtil.sendToSender(leakMessage, player);
        messageUtil.sendToPlayer(plugin.getMessageManager().getString("leak.exposed", "You have been exposed!"), target); // Original exposed message

        auditLogger.log("LEAK", player.getName(), target.getName(), price, "SUCCESS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Leaked " + target.getName() + "'s coordinates: " + coords);
    }

    private void handleShare(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageUtil.sendToSender(plugin.getMessageManager().getPlayerOnlyCommand(), sender);
            return;
        }

        if (!player.hasPermission("coordleak.share")) {
            messageUtil.sendToSender(plugin.getMessageManager().getNoPermission(), player);
            return;
        }

        // Blacklist/Whitelist check
        if (plugin.getConfigManager().isBlacklistEnabled() && protectionManager.isBlacklisted(player.getUniqueId())) {
            messageUtil.sendToSender(plugin.getMessageManager().getPlayerBlacklisted(), player);
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "BLACKLISTED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is blacklisted.");
            return;
        }
        if (plugin.getConfigManager().isWhitelistEnabled() && !protectionManager.isWhitelisted(player.getUniqueId())) {
            messageUtil.sendToSender(plugin.getMessageManager().getPlayerNotWhitelisted(), player);
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "NOT_WHITELISTED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is not whitelisted.");
            return;
        }

        // Cooldown check
        if (protectionManager.isOnCooldown(player.getUniqueId(), "share")) {
            long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "share");
            messageUtil.sendToSender(plugin.getMessageManager().getCommandCooldown().replace("%time%", formatTime(remaining)), player);
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is on cooldown.");
            return;
        }

        // Rate Limit check
        if (protectionManager.isRateLimited(player.getUniqueId(), "share")) {
            messageUtil.sendToSender(plugin.getMessageManager().getCommandRateLimited(), player);
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is rate limited.");
            return;
        }

        // Daily Limit check
        if (protectionManager.hasExceededDailyLimit(player.getUniqueId(), "share")) {
            messageUtil.sendToSender(plugin.getMessageManager().getDailyLimitExceeded(), player);
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "DAILY_LIMIT_EXCEEDED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player exceeded daily limit.");
            return;
        }

        if (args.length < 2) {
            handleHelp(sender);
            return;
        }
        Player targetShare = Bukkit.getPlayer(args[1]);
        if (targetShare == null) {
            messageUtil.sendToSender(plugin.getMessageManager().getTargetNotOnline(), player);
            auditLogger.log("SHARE_ATTEMPT", player.getName(), args[1], 0, "TARGET_NOT_ONLINE", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Target player not found or offline.");
            return;
        }
        if (player.getUniqueId().equals(targetShare.getUniqueId())) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("cannotTargetYourself", "Cannot target yourself!"), player); // Use existing message
            auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "TARGET_SELF", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player tried to share with self.");
            return;
        }

        // Target validation
        if (!protectionManager.isValidTarget(targetShare)) {
            // Specific messages for why target is invalid
            if (plugin.getConfigManager().getExcludedWorlds().contains(targetShare.getWorld().getName())) {
                messageUtil.sendToSender(plugin.getMessageManager().getTargetExcludedWorld(), player);
                auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "TARGET_EXCLUDED_WORLD", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Target in excluded world.");
            } else {
                messageUtil.sendToSender(plugin.getMessageManager().getTargetExcludedPermission(), player);
                auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "TARGET_EXCLUDED_PERMISSION", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Target has excluded permission.");
            }
            return;
        }

        // Target consent check
        if (plugin.getConfigManager().isConsentRequiredForShare() && !protectionManager.hasTargetConsent(targetShare.getUniqueId())) {
            messageUtil.sendToSender(plugin.getMessageManager().getTargetNoConsent(), player);
            auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "NO_CONSENT", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Target has not consented to share.");
            return;
        }

        // Record usage
        protectionManager.applyCooldown(player.getUniqueId(), "share");
        protectionManager.recordRateLimitUsage(player.getUniqueId(), "share");
        protectionManager.incrementDailyUsage(player.getUniqueId(), "share");

        String customText = null;
        if (args.length > 2) {
            customText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            // Sanitize custom text if not admin
            if (!player.hasPermission("coordleak.admin")) {
                customText = MiniMessage.miniMessage().stripTags(customText); // Strip MiniMessage tags
                customText = customText.replaceAll("%", "%%"); // Escape PlaceholderAPI %
            }
        }

        String coords = String.format("X: %d, Y: %d, Z: %d", player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());

        // Message to sender
        String senderMessage = plugin.getMessageManager().getShareSuccessSender()
                .replace("%player%", player.getName())
                .replace("%target%", targetShare.getName());
        messageUtil.sendToSender(senderMessage, player);

        // Message to target
        String targetMessage = plugin.getMessageManager().getShareSuccessTarget()
                .replace("%sender%", player.getName())
                .replace("%player%", player.getName()) // Player whose coords are being shared
                .replace("%coords%", coords);

        if (customText != null) {
            targetMessage += " " + customText; // Append custom text
        }

        messageUtil.sendToPlayer(targetMessage, targetShare);

        auditLogger.log("SHARE", player.getName(), targetShare.getName(), 0, "SUCCESS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Shared " + player.getName() + "'s coordinates with " + targetShare.getName() + ": " + coords + (customText != null ? " (Custom: " + customText + ")" : ""));
    }

    private void handleInfo(CommandSender sender) {
        List<String> infoMessages = plugin.getMessageManager().getStringList("info.layout", Collections.emptyList());
        if (infoMessages.isEmpty()) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("configError", "Config error!"), sender);
            return;
        }

        String pluginName = plugin.getDescription().getName();
        String version = plugin.getDescription().getVersion();
        String author = String.join(", ", plugin.getDescription().getAuthors());
        double defaultPrice = plugin.getConfigManager().getDefaultPrice();
        long leakCooldown = plugin.getConfigManager().getCooldown("leak");
        String papiStatus = plugin.hasPAPI() ? plugin.getMessageManager().getString("info.enabled", "Enabled") : plugin.getMessageManager().getString("info.disabled", "Disabled");
        String vaultStatus = plugin.getEconomy() != null ? plugin.getMessageManager().getString("info.enabled", "Enabled") : plugin.getMessageManager().getString("info.disabled", "Disabled");

        for (String msg : infoMessages) {
            msg = msg.replace("%plugin_name%", pluginName)
                    .replace("%plugin_version%", version)
                    .replace("%plugin_author%", author)
                    .replace("%leak_price%", String.format("%.2f", defaultPrice))
                    .replace("%leak_cooldown%", formatTime(leakCooldown))
                    .replace("%papi_status%", papiStatus)
                    .replace("%vault_status%", vaultStatus);
            plugin.audience(sender).sendMessage(MiniMessage.miniMessage().deserialize(msg));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String partial = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("coordleak.leak")) subCommands.add("leak");
            if (sender.hasPermission("coordleak.share")) subCommands.add("share");
            if (sender.hasPermission("coordleak.admin")) subCommands.add("reload");
            if (sender.hasPermission("coordleak.setprice")) subCommands.add("setprice");
            subCommands.add("info");

            subCommands.stream()
                    .filter(sub -> sub.startsWith(partial))
                    .forEach(completions::add);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("share") && sender.hasPermission("coordleak.share")) {
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .forEach(completions::add);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("coordleak.admin")) {
            if ("confirm".startsWith(partial)) {
                completions.add("confirm");
            }
        }

        return completions;
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        seconds %= 60;
        if (minutes < 60) {
            return minutes + "m " + seconds + "s";
        }
        long hours = minutes / 60;
        minutes %= 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }
}