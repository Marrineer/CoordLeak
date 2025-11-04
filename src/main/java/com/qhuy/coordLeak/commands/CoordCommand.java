package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.CooldownManager;
import com.qhuy.coordLeak.utils.CoordLeakExpansion;
import com.qhuy.coordLeak.utils.InfoStatus;
import com.qhuy.coordLeak.utils.Message;
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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CoordCommand implements CommandExecutor, TabCompleter {
    private final CoordLeak plugin;
    private final CoordLeakExpansion PAPI;
    private final CooldownManager CM;
    private final boolean PAPIEnabled;
    private double price;
    private long cooldown;
    private boolean isPlayer = false;

    public CoordCommand(CoordLeak plugin, CoordLeakExpansion PAPI, CooldownManager CM, boolean PAPIEnabled) {
        this.plugin = plugin;
        this.PAPI = PAPI;
        this.CM = CM;
        this.PAPIEnabled = PAPIEnabled;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        price = plugin.getConfig().getDouble("price", 500);
        cooldown = plugin.getConfig().getLong("settings.cooldown-per-usage", 300);

        isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        boolean isAdmin = isPlayer && (player.isOp() || player.hasPermission("coordleak.admin"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String subCommand = args.length > 0 ? args[0].toLowerCase() : "";

            switch (subCommand) {
                case "leak":
                    if (!isPlayer) {
                        Message.sendToSender(Message.get("onlyPlayer"), sender);
                        return;
                    }
                    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                    players.remove(player);
                    if (players.isEmpty()) {
                        Message.sendToSender(Message.get("noOneIsOnline"), sender);
                        return;
                    }
                    Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!isAdmin) {
                            if (!CM.usable(player, cooldown)) {
                                Message.sendToSender(Message.get("cooldownMessage"), sender);
                                return;
                            }
                            double balance = plugin.getEconomy().getBalance(player);
                            if (balance < price) {
                                Message.sendToSender(Message.get("notEnoughBalance"), sender);
                                return;
                            }
                            plugin.getEconomy().withdrawPlayer(player, price);
                            CM.setCooldown(player, cooldown);
                        }
                        sendStringList("randomSelect", sender, target);

                        Message.sendToPlayer(Message.get("leak.exposed"), target);
                    });
                    break;

                case "share":
                    if (!isPlayer) {
                        Message.sendToSender(Message.get("onlyPlayer"), sender);
                        return;
                    }
                    if (args.length < 2) {
                        sendStringList("help", sender, player);
                        return;
                    }
                    Player targetShare = Bukkit.getPlayer(args[1]);
                    if (targetShare == null) {
                        Message.sendToSender(Message.get("invalidPlayer"), sender);
                        return;
                    }
                    if(player.getUniqueId() == targetShare.getUniqueId()) {
                        Message.sendToSender(Message.get("cannotTargetYourself"), sender);
                        return;
                    }
                    String customText = null;
                    if (args.length > 2) {
                        customText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    }
                    List<String> strList = CoordLeak.getInstance().getMessage().getStringList("shareCoord");
                    if (customText != null) {
                        strList.set(1, customText);
                    }
                    if (strList.isEmpty()) {
                        String msg = Message.get("configError");
                        CoordLeak.getInstance().audience(targetShare).sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                        PAPIEnabled ? PlaceholderAPI.setPlaceholders(player, msg) : msg
                                )
                        );
                        return;
                    }
                    for (String msg : strList) {
                        CoordLeak.getInstance().audience(targetShare).sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                        PAPIEnabled ? PlaceholderAPI.setPlaceholders(player, msg) : msg
                                )
                        );
                    }

                case "reload":
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!sender.hasPermission("coordleak.admin")) {
                            Message.sendToSender(Message.get("permission"), sender);
                            return;
                        }
                        plugin.reloadConfig();
                        plugin.getMessageManager().reloadMessage();
                        PAPI.unregister();
                        PAPI.register();
                        Message.sendToSender(Message.get("configReloaded"), sender);
                        CoordLeak.getInstance().info(InfoStatus.RESTART);
                    });
                    break;

                default:
                    sendStringList("help", sender, player);
                    break;
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(List.of("leak", "share"));
            if (sender.hasPermission("coordleak.admin")) {
                subCommands.add("reload");
            }

            String partial = args[0].toLowerCase();
            completions.addAll(
                    subCommands.stream()
                            .filter(sub -> sub.startsWith(partial))
                            .toList()
            );
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("share")) {
            String partial = args[1].toLowerCase();
            completions.addAll(
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(partial))
                            .toList()
            );
        }

        return completions;
    }

    public void sendStringList(String alias, CommandSender sender, Player target) {
        List<String> StrList = CoordLeak.getInstance().getMessage().getStringList(alias);
        if (StrList.isEmpty()) {
            String msg = Message.get("configError");
            CoordLeak.getInstance().audience(sender).sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            PAPIEnabled ? PlaceholderAPI.setPlaceholders(target, msg) : msg
                    )
            );
            return;
        }
        for (String msg : StrList) {
            CoordLeak.getInstance().audience(sender).sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            PAPIEnabled ? PlaceholderAPI.setPlaceholders(target, msg) : msg
                    )
            );
        }
    }
}