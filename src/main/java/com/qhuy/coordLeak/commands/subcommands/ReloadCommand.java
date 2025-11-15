package com.qhuy.coordLeak.commands.subcommands;

import com.qhuy.coordLeak.CoordLeak;
import com.qhuy.coordLeak.managers.AuditLogger;
import com.qhuy.coordLeak.managers.ConfigManager;
import com.qhuy.coordLeak.managers.ProtectionManager;
import com.qhuy.coordLeak.utils.MessageUtil;
import com.qhuy.coordLeak.utils.Permissions;
import com.qhuy.coordLeak.utils.PlayerUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements SubCommand {

    private final CoordLeak plugin;
    private final ProtectionManager protectionManager;
    private final AuditLogger auditLogger;
    private final MessageUtil messageUtil;
    private final ConfigManager configManager;

    public ReloadCommand(CoordLeak plugin, ProtectionManager protectionManager, AuditLogger auditLogger) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
        this.auditLogger = auditLogger;
        this.messageUtil = plugin.getMessageUtil();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public @NotNull String getName() {
        return "reload";
    }

    @Override
    public @NotNull String getPermission() {
        return Permissions.ADMIN;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(getPermission())) {
            messageUtil.send(sender, "no-permission");
            return;
        }

        // Console can reload without confirmation
        if (!(sender instanceof Player player)) {
            plugin.reloadManagers();
            messageUtil.send(sender, "reload-confirmed");
            auditLogger.log("RELOAD", "Console", "N/A", 0, "SUCCESS", "N/A", "Plugin reloaded by console.");
            return;
        }

        String loggedIp = PlayerUtil.getLoggableIp(player, configManager.isIpLoggingEnabled());

        boolean confirmRequired = configManager.isReloadRequireConfirm();
        boolean confirmed = args.length > 0 && args[0].equalsIgnoreCase("confirm");

        if (confirmRequired && !confirmed) {
            String commandToConfirm = "reload";
            String time = String.valueOf(configManager.getConfirmationTimeout(commandToConfirm) / 1000);
            protectionManager.initiateConfirmation(player.getUniqueId(), commandToConfirm);
            messageUtil.send(player, "reload-confirm-required", "%command%", commandToConfirm, "%time%", time);
            auditLogger.log("RELOAD_ATTEMPT", player.getName(), "N/A", 0, "CONFIRM_PENDING", loggedIp, "Player initiated reload confirmation.");
            return;
        }

        if (confirmRequired) {
            if (!protectionManager.hasPendingConfirmation(player.getUniqueId(), "reload")) {
                messageUtil.send(player, "reload-cancelled");
                auditLogger.log("RELOAD_ATTEMPT", player.getName(), "N/A", 0, "CONFIRM_EXPIRED", loggedIp, "Player tried to confirm reload but no pending confirmation.");
                return;
            }
            protectionManager.clearConfirmation(player.getUniqueId(), "reload");
        }

        plugin.reloadManagers();
        messageUtil.send(player, "reload-confirmed");
        auditLogger.log("RELOAD", player.getName(), "N/A", 0, "SUCCESS", loggedIp, "Plugin reloaded by player.");
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission(getPermission())) {
            if ("confirm".startsWith(args[0].toLowerCase())) {
                return Collections.singletonList("confirm");
            }
        }
        return Collections.emptyList();
    }
}
