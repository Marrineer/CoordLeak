package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.AuditLogger;
import com.qhuy.coordLeak.managers.ProtectionManager;
import com.qhuy.coordLeak.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
            case "setprice":
                handleSetPrice(sender, args);
                break;
            default:
                handleHelp(sender);
                break;
        }
        return true;
    }

    private void handleHelp(CommandSender sender) {
        // The 'help.layout' key in messages.yml should contain the help message.
        messageUtil.sendList(sender, "info.layout");
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coordleak.admin")) {
            messageUtil.send(sender, "no-permission");
            return;
        }

        if (!(sender instanceof Player player)) {
            plugin.reloadManagers();
            messageUtil.send(sender, "reload-confirmed");
            auditLogger.log("RELOAD", sender.getName(), "N/A", 0, "SUCCESS", "N/A", "Plugin reloaded by console.");
            return;
        }

        boolean confirmRequired = plugin.getConfigManager().isReloadRequireConfirm();
        boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");

        if (confirmRequired && !confirmed) {
            String commandToConfirm = "reload";
            String time = String.valueOf(plugin.getConfigManager().getConfirmationTimeout(commandToConfirm) / 1000);
            protectionManager.initiateConfirmation(player.getUniqueId(), commandToConfirm);
            messageUtil.send(player, "reload-confirm-required", "%command%", commandToConfirm, "%time%", time);
            auditLogger.log("RELOAD_ATTEMPT", player.getName(), "N/A", 0, "CONFIRM_PENDING", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player initiated reload confirmation.");
            return;
        }

        if (confirmRequired) { // Confirmed is true if we reach here
            if (!protectionManager.hasPendingConfirmation(player.getUniqueId(), "reload")) {
                messageUtil.send(player, "reload-cancelled");
                auditLogger.log("RELOAD_ATTEMPT", player.getName(), "N/A", 0, "CONFIRM_EXPIRED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player tried to confirm reload but no pending confirmation.");
                return;
            }
            protectionManager.clearConfirmation(player.getUniqueId(), "reload");
        }

        plugin.reloadManagers();
        messageUtil.send(player, "reload-confirmed");
        auditLogger.log("RELOAD", player.getName(), "N/A", 0, "SUCCESS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Plugin reloaded by player.");
    }

    private void handleSetPrice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coordleak.setprice")) {
            messageUtil.send(sender, "no-permission");
            return;
        }

        if (sender instanceof Player player) {
            if (protectionManager.isOnCooldown(player.getUniqueId(), "setprice")) {
                long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "setprice");
                messageUtil.send(player, "command-cooldown", "%time%", formatTime(remaining));
                auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is on cooldown for setprice.");
                return;
            }
            if (protectionManager.isRateLimited(player.getUniqueId(), "setprice")) {
                messageUtil.send(player, "command-rate-limited");
                auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is rate limited for setprice.");
                return;
            }
        }

        if (args.length < 2) {
            double currentPrice = plugin.getConfigManager().getDefaultPrice();
            messageUtil.send(sender, "current-price", "%price%", String.format("%.2f", currentPrice));
            return;
        }

        double newPrice;
        try {
            newPrice = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            messageUtil.send(sender, "invalid-price");
            auditLogger.log("SET_PRICE_ATTEMPT", sender.getName(), args[1], 0, "INVALID_NUMBER_FORMAT", sender instanceof Player ? ((Player) sender).getAddress().getAddress().getHostAddress() : "N/A", "Invalid number format for price.");
            return;
        }

        if (newPrice < plugin.getConfigManager().getMinPrice() || newPrice > plugin.getConfigManager().getMaxPrice()) {
            messageUtil.send(sender, "price-out-of-range",
                    "%min_price%", String.format("%.2f", plugin.getConfigManager().getMinPrice()),
                    "%max_price%", String.format("%.2f", plugin.getConfigManager().getMaxPrice()));
            auditLogger.log("SET_PRICE_ATTEMPT", sender.getName(), String.valueOf(newPrice), newPrice, "PRICE_OUT_OF_RANGE", sender instanceof Player ? ((Player) sender).getAddress().getAddress().getHostAddress() : "N/A", "Price out of configured range.");
            return;
        }

        if (sender instanceof Player player) {
            boolean confirmRequired = plugin.getConfigManager().isReloadRequireConfirm();
            boolean confirmed = args.length > 2 && args[2].equalsIgnoreCase("confirm");

            if (confirmRequired && !confirmed) {
                String commandToConfirm = "setprice " + args[1];
                String time = String.valueOf(plugin.getConfigManager().getConfirmationTimeout("setprice") / 1000);
                protectionManager.initiateConfirmation(player.getUniqueId(), "setprice");
                messageUtil.send(player, "reload-confirm-required", "%command%", commandToConfirm, "%time%", time);
                auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), String.valueOf(newPrice), newPrice, "CONFIRM_PENDING", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player initiated setprice confirmation.");
                return;
            }

            if (confirmRequired) { // confirmed is true
                if (!protectionManager.hasPendingConfirmation(player.getUniqueId(), "setprice")) {
                    messageUtil.send(player, "reload-cancelled");
                    auditLogger.log("SET_PRICE_ATTEMPT", player.getName(), String.valueOf(newPrice), newPrice, "CONFIRM_EXPIRED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player tried to confirm setprice but no pending confirmation.");
                    return;
                }
                protectionManager.clearConfirmation(player.getUniqueId(), "setprice");
            }
        }

        plugin.getConfigManager().setDefaultPrice(newPrice);
        if (sender instanceof Player player) {
            protectionManager.applyCooldown(player.getUniqueId(), "setprice");
            protectionManager.recordRateLimitUsage(player.getUniqueId(), "setprice");
        }
        messageUtil.send(sender, "setprice-success", "%price%", String.format("%.2f", newPrice));
        auditLogger.log("SET_PRICE", sender.getName(), "N/A", newPrice, "SUCCESS", sender instanceof Player ? ((Player) sender).getAddress().getAddress().getHostAddress() : "N/A", "Price set to " + newPrice);
    }

    private void handleLeak(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageUtil.send(sender, "player-only-command");
            return;
        }

        if (!player.hasPermission("coordleak.leak")) {
            messageUtil.send(player, "no-permission");
            return;
        }

        if (plugin.getConfigManager().isBlacklistEnabled() && protectionManager.isBlacklisted(player.getUniqueId())) {
            messageUtil.send(player, "player-blacklisted");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "BLACKLISTED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is blacklisted.");
            return;
        }
        if (plugin.getConfigManager().isWhitelistEnabled() && !protectionManager.isWhitelisted(player.getUniqueId())) {
            messageUtil.send(player, "player-not-whitelisted");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "NOT_WHITELISTED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is not whitelisted.");
            return;
        }

        if (plugin.getConfigManager().isGlobalRateLimitEnabled() && protectionManager.isGlobalRateLimited()) {
            long remaining = protectionManager.getGlobalBlockRemaining();
            messageUtil.send(player, "global-rate-limited", "%time%", formatTime(remaining));
            String logMessage = plugin.getMessageManager().getString("global-limit-exceeded-log", "[CoordLeak] Global rate limit exceeded. Blocking requests for %time% seconds.");
            plugin.getLogger().warning(logMessage.replace("%time%", String.valueOf(plugin.getConfigManager().getGlobalRateLimitBlockDuration() / 1000)));
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "GLOBAL_RATE_LIMITED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Global rate limit exceeded.");
            return;
        }

        if (protectionManager.isOnCooldown(player.getUniqueId(), "leak")) {
            long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "leak");
            messageUtil.send(player, "command-cooldown", "%time%", formatTime(remaining));
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is on cooldown.");
            return;
        }

        if (protectionManager.isRateLimited(player.getUniqueId(), "leak")) {
            messageUtil.send(player, "command-rate-limited");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is rate limited.");
            return;
        }

        if (protectionManager.hasExceededDailyLimit(player.getUniqueId(), "leak")) {
            messageUtil.send(player, "daily-limit-exceeded");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", 0, "DAILY_LIMIT_EXCEEDED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player exceeded daily limit.");
            return;
        }

        double price = plugin.getConfigManager().getDefaultPrice();

        if (!protectionManager.hasBypassPermission(player.getUniqueId(), "admin")) {
            if (plugin.getEconomy() == null) {
                messageUtil.send(player, "economy-error");
                auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "ECONOMY_ERROR", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Economy not available.");
                return;
            }
            if (plugin.getEconomy().getBalance(player) < price) {
                messageUtil.send(player, "insufficient-funds", "%amount%", String.format("%.2f", price));
                auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "INSUFFICIENT_FUNDS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player has insufficient funds.");
                return;
            }

            synchronized (player.getUniqueId().toString().intern()) {
                if (plugin.getEconomy().getBalance(player) < price) {
                    messageUtil.send(player, "insufficient-funds", "%amount%", String.format("%.2f", price));
                    auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "INSUFFICIENT_FUNDS_CONCURRENT", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player has insufficient funds due to concurrent access.");
                    return;
                }

                if (!plugin.getEconomy().withdrawPlayer(player, price).transactionSuccess()) {
                    messageUtil.send(player, "economy-error");
                    auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "WITHDRAW_FAILED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Economy withdrawal failed.");
                    return;
                }
            }
        }

        List<Player> potentialTargets = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .filter(protectionManager::isValidTarget)
                .collect(Collectors.toList());

        if (potentialTargets.isEmpty()) {
            messageUtil.send(player, "no-leak-target-found");
            auditLogger.log("LEAK_ATTEMPT", player.getName(), "N/A", price, "NO_TARGET_FOUND", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "No suitable target player found.");
            if (!protectionManager.hasBypassPermission(player.getUniqueId(), "admin") && plugin.getEconomy() != null) {
                plugin.getEconomy().depositPlayer(player, price);
                auditLogger.log("LEAK_REFUND", player.getName(), "N/A", price, "SUCCESS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Refunded due to no target found.");
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

        auditLogger.log("LEAK", player.getName(), target.getName(), price, "SUCCESS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Leaked " + target.getName() + "'s coordinates: " + coords);
    }

    private void handleShare(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageUtil.send(sender, "player-only-command");
            return;
        }

        if (!player.hasPermission("coordleak.share")) {
            messageUtil.send(player, "no-permission");
            return;
        }

        if (plugin.getConfigManager().isBlacklistEnabled() && protectionManager.isBlacklisted(player.getUniqueId())) {
            messageUtil.send(player, "player-blacklisted");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "BLACKLISTED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is blacklisted.");
            return;
        }
        if (plugin.getConfigManager().isWhitelistEnabled() && !protectionManager.isWhitelisted(player.getUniqueId())) {
            messageUtil.send(player, "player-not-whitelisted");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "NOT_WHITELISTED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is not whitelisted.");
            return;
        }

        if (protectionManager.isOnCooldown(player.getUniqueId(), "share")) {
            long remaining = protectionManager.getRemainingCooldown(player.getUniqueId(), "share");
            messageUtil.send(player, "command-cooldown", "%time%", formatTime(remaining));
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "COOLDOWN", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is on cooldown.");
            return;
        }

        if (protectionManager.isRateLimited(player.getUniqueId(), "share")) {
            messageUtil.send(player, "command-rate-limited");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "RATE_LIMITED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player is rate limited.");
            return;
        }

        if (protectionManager.hasExceededDailyLimit(player.getUniqueId(), "share")) {
            messageUtil.send(player, "daily-limit-exceeded");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), "N/A", 0, "DAILY_LIMIT_EXCEEDED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player exceeded daily limit.");
            return;
        }

        if (args.length < 2) {
            handleHelp(sender);
            return;
        }
        Player targetShare = Bukkit.getPlayer(args[1]);
        if (targetShare == null) {
            messageUtil.send(player, "target-not-online");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), args[1], 0, "TARGET_NOT_ONLINE", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Target player not found or offline.");
            return;
        }
        if (player.getUniqueId().equals(targetShare.getUniqueId())) {
            messageUtil.send(player, "cannot-target-yourself");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "TARGET_SELF", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Player tried to share with self.");
            return;
        }

        if (!protectionManager.isValidTarget(targetShare)) {
            if (plugin.getConfigManager().getExcludedWorlds().contains(targetShare.getWorld().getName())) {
                messageUtil.send(player, "target-excluded-world");
                auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "TARGET_EXCLUDED_WORLD", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Target in excluded world.");
            } else {
                messageUtil.send(player, "target-excluded-permission");
                auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "TARGET_EXCLUDED_PERMISSION", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Target has excluded permission.");
            }
            return;
        }

        if (plugin.getConfigManager().isConsentRequiredForShare() && !protectionManager.hasTargetConsent(targetShare.getUniqueId())) {
            messageUtil.send(player, "target-no-consent");
            auditLogger.log("SHARE_ATTEMPT", player.getName(), targetShare.getName(), 0, "NO_CONSENT", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Target has not consented to share.");
            return;
        }

        protectionManager.applyCooldown(player.getUniqueId(), "share");
        protectionManager.recordRateLimitUsage(player.getUniqueId(), "share");
        protectionManager.incrementDailyUsage(player.getUniqueId(), "share");

        String customText = "";
        if (args.length > 2) {
            String rawText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            if (!player.hasPermission("coordleak.admin")) {
                customText = " " + plugin.getSanitizer().sanitize(rawText);
            } else {
                customText = " " + rawText;
            }
        }

        String coords = String.format("X: %d, Y: %d, Z: %d", player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());

        messageUtil.send(player, "share-success-sender", "%target%", targetShare.getName());
        messageUtil.send(targetShare, "share-success-target", "%sender%", player.getName(), "%coords%", coords + customText);

        auditLogger.log("SHARE", player.getName(), targetShare.getName(), 0, "SUCCESS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Shared " + player.getName() + "'s coordinates with " + targetShare.getName() + ": " + coords + (!customText.isEmpty() ? " (Custom:" + customText + ")" : ""));
    }

    private void handleInfo(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        String pluginName = plugin.getDescription().getName();
        String version = plugin.getDescription().getVersion();
        String author = String.join(", ", plugin.getDescription().getAuthors());
        double defaultPrice = plugin.getConfigManager().getDefaultPrice();
        long leakCooldown = plugin.getConfigManager().getCooldownMillis("leak");
        String papiStatus = plugin.hasPAPI() ? plugin.getMessageManager().getFormattedString("info.enabled", player) : plugin.getMessageManager().getFormattedString("info.disabled", player);
        String vaultStatus = plugin.getEconomy() != null ? plugin.getMessageManager().getFormattedString("info.enabled", player) : plugin.getMessageManager().getFormattedString("info.disabled", player);

        messageUtil.sendList(sender, "info.layout",
                "%plugin_name%", pluginName,
                "%plugin_version%", version,
                "%plugin_author%", author,
                "%leak_price%", String.format("%.2f", defaultPrice),
                "%leak_cooldown%", formatTime(leakCooldown),
                "%papi_status%", papiStatus,
                "%vault_status%", vaultStatus
        );
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
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("share") && sender.hasPermission("coordleak.share")) {
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .forEach(completions::add);
            } else if ((args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("setprice")) && sender.hasPermission("coordleak.admin")) {
                if ("confirm".startsWith(partial)) {
                    completions.add("confirm");
                }
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