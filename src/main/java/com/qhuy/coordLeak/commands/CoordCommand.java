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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CoordCommand implements CommandExecutor {
    private final CoordLeak plugin;
    private final CoordLeakExpansion PAPI;
    private final CooldownManager CM;
    private double price;
    private long cooldown;

    public CoordCommand(CoordLeak plugin, CoordLeakExpansion PAPI, CooldownManager CM) {
        this.plugin = plugin;
        this.PAPI = PAPI;
        this.CM = CM;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        price = plugin.getConfig().getDouble("price", 500);
        cooldown = plugin.getConfig().getLong("cooldown-per-usage", 300);
        if (!(sender instanceof Player player)) {
            Message.sendToSender(Message.get("onlyPlayer"), sender);
            return true;
        }
        boolean isAdmin = player.isOp() || player.hasPermission("coordleak.admin");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            switch (Arrays.toString(args)) {
                case "use":
                    List<String> messages = CoordLeak.getInstance().getConfig().getStringList("randomSelect");
                    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                    players.remove(player);
                    if (players.isEmpty()) {
                        Message.sendToSender(Message.get("noOneIsOnline"), sender);
                        return;
                    }
                    Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                    if (!isAdmin) {
                        if (!CM.usable(player, cooldown)) {
                            double balance = plugin.getEconomy().getBalance(player);
                            if (balance < price) {
                                Message.sendToSender(Message.get("notEnoughBalance"), sender);
                                return;
                            }
                            plugin.getEconomy().withdrawPlayer(player, price);
                            CM.setCooldown(player, cooldown);
                            Message.sendToPlayer(Message.get("leak.exposed"), target);
                        }
                    }
                    if (messages.isEmpty()) {
                        CoordLeak.getInstance().audience(sender).sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                        PlaceholderAPI.setPlaceholders(
                                                player,
                                                Message.get("configError")
                                        )
                                )
                        );
                        return;
                    }
                    for (String msg : messages) {
                        CoordLeak.getInstance().audience(sender).sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                        PlaceholderAPI.setPlaceholders(
                                                player,
                                                msg
                                        )
                                )
                        );
                    }
                    break;
                case "reload":
                    if (!(sender.hasPermission("coordleak.admin"))) {
                        Message.sendToSender(Message.get("permission"), sender);
                        return;
                    }
                    plugin.reloadConfig();
                    plugin.getMessageManager().reloadMessage();

                    PAPI.unregister();
                    PAPI.register();

                    Message.sendToSender(Message.get("configReloaded"), sender);
                    break;
                default:
                    List<String> help = CoordLeak.getInstance().getConfig().getStringList("help");
                    if (help.isEmpty()) {
                        CoordLeak.getInstance().audience(sender).sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                        PlaceholderAPI.setPlaceholders(
                                                player,
                                                Message.get("configError")
                                        )
                                )
                        );
                        return;
                    }
                    for (String msg : help) {
                        CoordLeak.getInstance().audience(sender).sendMessage(
                                MiniMessage.miniMessage().deserialize(
                                        PlaceholderAPI.setPlaceholders(
                                                player,
                                                msg
                                        )
                                )
                        );
                    }
            }
        });

        return true;
    }
}
