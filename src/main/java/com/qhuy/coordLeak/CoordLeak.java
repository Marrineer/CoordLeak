package com.qhuy.coordLeak;

import com.qhuy.coordLeak.commands.CoordCommand;
import com.qhuy.coordLeak.managers.AuditLogger;
import com.qhuy.coordLeak.managers.ConfigManager;
import com.qhuy.coordLeak.managers.MessageManager;
import com.qhuy.coordLeak.managers.ProtectionManager;
import com.qhuy.coordLeak.utils.CoordLeakExpansion;
import com.qhuy.coordLeak.utils.InfoStatus;
import com.qhuy.coordLeak.utils.MessageUtil;
import com.qhuy.coordLeak.utils.Sanitizer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class CoordLeak extends JavaPlugin {
    private BukkitAudiences adventure;
    private Economy econ;
    private MessageManager messageManager;
    private ConfigManager configManager;
    private ProtectionManager protectionManager;
    private AuditLogger auditLogger;
    private MessageUtil messageUtil;
    private Sanitizer sanitizer;
    private CoordLeakExpansion PAPI;
    private boolean PAPIEnabled;
    private BukkitTask cleanupTask;

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize managers
        this.adventure = BukkitAudiences.create(this);
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.protectionManager = new ProtectionManager(this, configManager); // Pass configManager
        this.auditLogger = new AuditLogger(this, configManager); // Pass configManager
        this.messageUtil = new MessageUtil(this);
        this.sanitizer = new Sanitizer();

        // Check for PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PAPI = new CoordLeakExpansion(this);
            PAPIEnabled = PAPI.register();
            getLogger().info("PlaceholderAPI found and hooked.");
        } else {
            getLogger().info("PlaceholderAPI not found, skipping hook.");
        }

        // Check for Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault not found or no economy plugin, disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Vault found and hooked.");

        // Start cleanup task for ProtectionManager
        long cleanupInterval = 20L * 300; // 5 minutes
        this.cleanupTask = Bukkit.getScheduler().runTaskTimer(this, protectionManager::cleanup, cleanupInterval, cleanupInterval);

        // Register commands
        Bukkit.getPluginCommand("coord").setExecutor(
                new CoordCommand(this, protectionManager, auditLogger) // Pass new managers
        );

        info(InfoStatus.START);
    }

    @Override
    public void onDisable() {
        info(InfoStatus.STOP);

        if (PAPI != null && PAPI.isRegistered()) {
            PAPI.unregister();
        }

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        if (this.cleanupTask != null) {
            this.cleanupTask.cancel();
        }
        if (this.auditLogger != null) {
            this.auditLogger.shutdown(); // Shutdown audit logger executor
        }
        saveConfig();
    }

    public void reloadManagers() {
        configManager.loadConfig();
        messageManager.reloadMessages();
        // ProtectionManager reloads its config values on next check, but we can force a reload if needed
        // For now, just ensure configManager is reloaded.
        // If ProtectionManager had internal state that needed resetting based on config, a reload method would be here.

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            if (PAPI != null) {
                PAPI.unregister();
            }
            PAPI = new CoordLeakExpansion(this);
            PAPIEnabled = PAPI.register();
        } else {
            PAPIEnabled = false;
            if (PAPI != null) {
                PAPI.unregister();
                PAPI = null;
            }
        }
    }


    public void info(InfoStatus status) {
        boolean isEnabling = status.getStatus();
        String statusMessage = isEnabling ? "<green>Enabling" : "<red>Disabling";

        StringBuilder text = new StringBuilder("\n\n");
        text.append("<gray>[]===========[ <reset>").append(statusMessage).append(" CoordLeak<reset><gray>]===========[]\n");
        text.append("<gray>|<reset>\n");
        text.append("<gray>|<reset> <b><gradient:#FFFFFF:#C81EEC>Information</gradient></b><reset>:\n");
        text.append("<gray>|<reset>\n");
        text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>Name</gradient><reset>: <white>").append(getDescription().getName()).append("\n");
        text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>Version</gradient><reset>: <white>").append(getDescription().getVersion()).append("\n");
        text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>Author</gradient><reset>: <white>").append(String.join(", ", getDescription().getAuthors())).append("\n");
        text.append("<gray>|<reset>\n");

        if (isEnabling) {
            text.append("<gray>|<reset> <b><gradient:#FFFFFF:#C81EEC>Status</gradient></b><reset>:\n");
            text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>Vault</gradient><reset>: ").append(econ != null ? "<green>Enabled\n" : "<red>Disabled\n");
            text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>PlaceholderAPI</gradient><reset>: ").append(PAPIEnabled ? "<green>Enabled\n" : "<red>Disabled\n");
            text.append("<gray>|<reset>\n");
        }

        text.append("<dark_gray>[]=========================================[]</dark_gray>\n");

        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(text.toString()));
    }


    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    // Manager Getters
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    // Removed CooldownManager getter
    // public CooldownManager getCooldownManager() {
    //     return cooldownManager;
    // }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public AuditLogger getAuditLogger() {
        return auditLogger;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public Sanitizer getSanitizer() {
        return sanitizer;
    }

    public Economy getEconomy() {
        return econ;
    }

    public Audience audience(CommandSender sender) {
        return adventure().sender(sender);
    }

    public Audience audience(Player player) {
        return adventure().player(player);
    }

    public boolean hasPAPI() {
        return PAPIEnabled;
    }
}


