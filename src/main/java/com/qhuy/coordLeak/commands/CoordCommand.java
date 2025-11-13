package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.CooldownManager;
import com.qhuy.coordLeak.utils.MessageUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CoordCommand implements CommandExecutor, TabCompleter {
    private final CoordLeak plugin;
    private final CooldownManager cooldownManager;
    private final MessageUtil messageUtil;

    public CoordCommand(CoordLeak plugin) {
        this.plugin = plugin;
        this.cooldownManager = plugin.getCooldownManager();
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
                handleReload(sender);
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

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("coordleak.admin")) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("permission", "No permission!"), sender);
            return;
        }
        plugin.reloadManagers();
        messageUtil.sendToSender(plugin.getMessageManager().getString("configReloaded", "Config reloaded!"), sender);
        plugin.getLogger().info("Configuration and messages reloaded.");
    }

    private void handleLeak(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("onlyPlayer", "Only players can use this command!"), sender);
            return;
        }

        if (!player.hasPermission("coordleak.leak")) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("permission", "No permission!"), sender);
            return;
        }

        // Admin bypass
        if (!player.hasPermission("coordleak.admin")) {
            if (cooldownManager.isOnCooldown(player)) {
                messageUtil.sendToSender(plugin.getMessageManager().getString("cooldownMessage", "You are on cooldown!"), sender);
                return;
            }
            double price = plugin.getConfigManager().getPrice();
            double balance = plugin.getEconomy().getBalance(player);
            if (balance < price) {
                messageUtil.sendToSender(plugin.getMessageManager().getString("notEnoughBalance", "Not enough money!"), sender);
                return;
            }
            plugin.getEconomy().withdrawPlayer(player, price);
            cooldownManager.setCooldown(player, plugin.getConfigManager().getCooldown());
        }

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(player);
        if (players.isEmpty()) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("noOneIsOnline", "No one is online!"), sender);
            return;
        }
        Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));

        List<String> strList = plugin.getMessageManager().getStringList("randomSelect", Collections.emptyList());
        if (strList.isEmpty()) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("configError", "Config error!"), sender);
            return;
        }
        for (String msg : strList) {
            String processedMsg = plugin.hasPAPI() && target != null ? PlaceholderAPI.setPlaceholders(target, msg) : msg;
            plugin.audience(sender).sendMessage(MiniMessage.miniMessage().deserialize(processedMsg));
        }
        messageUtil.sendToPlayer(plugin.getMessageManager().getString("leak.exposed", "You have been exposed!"), target);
    }

    private void handleShare(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("onlyPlayer", "Only players can use this command!"), sender);
            return;
        }

        if (!player.hasPermission("coordleak.share")) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("permission", "No permission!"), sender);
            return;
        }

        if (args.length < 2) {
            handleHelp(sender);
            return;
        }
        Player targetShare = Bukkit.getPlayer(args[1]);
        if (targetShare == null) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("invalidPlayer", "Invalid player!"), sender);
            return;
        }
        if (player.getUniqueId().equals(targetShare.getUniqueId())) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("cannotTargetYourself", "Cannot target yourself!"), sender);
            return;
        }
        String customText = null;
        if (args.length > 2) {
            customText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        }
        List<String> strList = new ArrayList<>(plugin.getMessageManager().getStringList("shareCoord", Collections.emptyList()));
        if (customText != null && strList.size() > 1) {
            strList.set(1, customText);
        }

        if (strList.isEmpty()) {
            messageUtil.sendToSender(plugin.getMessageManager().getString("configError", "Config error!"), sender);
            return;
        }

        for (String msg : strList) {
            String processedMsg = plugin.hasPAPI() ? PlaceholderAPI.setPlaceholders(player, msg) : msg;
            plugin.audience(targetShare).sendMessage(MiniMessage.miniMessage().deserialize(processedMsg));
        }
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
        double price = plugin.getConfigManager().getPrice();
        long cooldown = plugin.getConfigManager().getCooldown();
        String papiStatus = plugin.hasPAPI() ? plugin.getMessageManager().getString("info.enabled", "Enabled") : plugin.getMessageManager().getString("info.disabled", "Disabled");
        String vaultStatus = plugin.getEconomy() != null ? plugin.getMessageManager().getString("info.enabled", "Enabled") : plugin.getMessageManager().getString("info.disabled", "Disabled");

        for (String msg : infoMessages) {
            msg = msg.replace("%plugin_name%", pluginName)
                    .replace("%plugin_version%", version)
                    .replace("%plugin_author%", author)
                    .replace("%leak_price%", String.valueOf(price))
                    .replace("%leak_cooldown%", String.valueOf(cooldown))
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
            if (sender.hasPermission("coordleak.admin")) subCommands.add("setprice"); // Add setprice to tab completion
            subCommands.add("info"); // Add info to tab completion

            subCommands.stream()
                    .filter(sub -> sub.startsWith(partial))
                    .forEach(completions::add);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("share") && sender.hasPermission("coordleak.share")) {
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .forEach(completions::add);
        }

        return completions;
    }
}