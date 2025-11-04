package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.CooldownManager;
import com.qhuy.coordLeak.utils.CoordLeakExpansion;
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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CoordCommand implements CommandExecutor, TabCompleter {
    private final CoordLeak plugin;
    private final CoordLeakExpansion PAPI;
    private final CooldownManager CM;
    private double price;
    private long cooldown;
    private final boolean PAPIEnabled;

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

        if (!(sender instanceof Player player)) {
            Message.sendToSender(Message.get("onlyPlayer"), sender);
            return true;
        }

        boolean isAdmin = player.isOp() || player.hasPermission("coordleak.admin");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String subCommand = args.length > 0 ? args[0].toLowerCase() : "";

            switch (subCommand) {
                case "use":
                    List<String> messages = CoordLeak.getInstance().getConfig().getStringList("randomSelect");
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

                        if (messages.isEmpty()) {
                            String msg = Message.get("configError");
                            CoordLeak.getInstance().audience(sender).sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                            PAPIEnabled ? PlaceholderAPI.setPlaceholders(player, msg) : msg
                                    )
                            );
                            return;
                        }

                        for (String msg : messages) {
                            CoordLeak.getInstance().audience(sender).sendMessage(
                                    MiniMessage.miniMessage().deserialize(
                                            PAPIEnabled ? PlaceholderAPI.setPlaceholders(player, msg) : msg
                                    )
                            );
                        }

                        Message.sendToPlayer(Message.get("leak.exposed"), target);
                    });
                    break;

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
                    });
                    break;

                default:
                    List<String> help = CoordLeak.getInstance().getConfig().getStringList("help");
                    if (help.isEmpty()) {
                        String msg = Message.get("configError");
                        CoordLeak.getInstance().audience(sender).sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                        PAPIEnabled ? PlaceholderAPI.setPlaceholders(player, msg) : msg
                                )
                        );
                        return;
                    }
                    for (String msg : help) {
                        CoordLeak.getInstance().audience(sender).sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                        PAPIEnabled ? PlaceholderAPI.setPlaceholders(player, msg) : msg
                                )
                        );
                    }
                    break;
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> subCommands = new ArrayList<>(List.of("use"));
            if (sender.hasPermission("coordleak.admin")) {
                subCommands.add("reload");
            }
            for (String sub : subCommands) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        }
        return completions;
    }
}