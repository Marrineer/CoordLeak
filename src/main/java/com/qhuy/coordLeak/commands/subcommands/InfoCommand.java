package com.qhuy.coordLeak.commands.subcommands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.ConfigManager;
import com.qhuy.coordLeak.managers.MessageManager;
import com.qhuy.coordLeak.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class InfoCommand implements SubCommand {

    private final CoordLeak plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final MessageUtil messageUtil;

    public InfoCommand(CoordLeak plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.messageUtil = plugin.getMessageUtil();
    }

    @Override
    public @NotNull String getName() {
        return "info";
    }

    @Override
    public @Nullable String getPermission() {
        return null; // No permission required for info
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        String pluginName = plugin.getDescription().getName();
        String version = plugin.getDescription().getVersion();
        String author = String.join(", ", plugin.getDescription().getAuthors());
        double defaultPrice = configManager.getDefaultPrice();
        long leakCooldown = configManager.getCooldownMillis("leak");
        String papiStatus = plugin.hasPAPI() ? messageManager.getFormattedString("info.enabled", player) : messageManager.getFormattedString("info.disabled", player);
        String vaultStatus = plugin.getEconomy() != null ? messageManager.getFormattedString("info.enabled", player) : messageManager.getFormattedString("info.disabled", player);

        messageUtil.sendList(sender, "info.layout",
                "%plugin_name%", pluginName,
                "%plugin_version%", version,
                "%plugin_author%", author,
                "%leak_price%", String.format("%.2f", defaultPrice),
                "%leak_cooldown%", MessageUtil.formatTime(leakCooldown),
                "%papi_status%", papiStatus,
                "%vault_status%", vaultStatus
        );
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
