package com.qhuy.coordLeak;

import com.qhuy.coordLeak.commands.CoordCommand;
import com.qhuy.coordLeak.managers.CooldownManager;
import com.qhuy.coordLeak.managers.MessageManager;
import com.qhuy.coordLeak.utils.CoordLeakExpansion;
import com.qhuy.coordLeak.utils.InfoStatus;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class CoordLeak extends JavaPlugin {
    private static CoordLeak instance;
    private BukkitAudiences adventure;
    private Economy econ;
    private MessageManager messageManager;
    private CooldownManager cooldownManager;
    private CoordLeakExpansion PAPI;
    private File file;
    private long cooldown;
    private boolean PAPIEnabled;
    private boolean ECONEnabled;


    public static CoordLeak getInstance() {
        return instance;
    }

    public BukkitAudiences adventure() {
        return this.adventure;
    }


    @Override
    public void onEnable() {
        saveDefaultConfig();

        cooldown = getConfig().getLong("settings.clean-interval", 300L);

        instance = this;
        this.adventure = BukkitAudiences.create(this);
        this.messageManager = new MessageManager(this);
        this.cooldownManager = new CooldownManager();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PAPI = new CoordLeakExpansion();
            PAPIEnabled = PAPI.register();
        }

        if (!setupEconomy()) {
            getLogger().warning("Could not setup Economy, disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        ECONEnabled = true;

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            cooldownManager.cleanup();
        }, cooldown * 20, cooldown * 20);

        // Register command
        Bukkit.getPluginCommand("coord").setExecutor(
                new CoordCommand(
                        this,
                        PAPI,
                        cooldownManager,
                        PAPIEnabled
                )
        );
        info(InfoStatus.START);
    }

    @Override
    public void onDisable() {
        info(InfoStatus.STOP);
        if (PAPI != null) {
            PAPI.unregister();
        }
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        saveConfig();
    }

    public void info(InfoStatus status) {
        StringBuilder text = new StringBuilder("\n\n");
        text.append("<gray>[]===========[<reset>").append(status.getMessage()).append(" CoordLeak<reset><gray>]===========[]\n");
        text.append("<gray>|<reset>\n");
        text.append("<gray>|<reset> <b><gradient:#FFFFFF:#C81EEC>Information</gradient></b><reset>:\n");
        text.append("<gray>|<reset>\n");
        text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>Name</gradient><reset>: <white>CoordLeak\n");
        text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>Author</gradient><reset>: <white>").append(getDescription().getAuthors()).append("\n");
        text.append("<gray>|<reset>\n");
        text.append("<gray>|<reset> <b><gradient:#FFFFFF:#C81EEC>Contact</gradient></b><reset>:\n");
        text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>Email</gradient><reset>: <white>marrineer@gmail.com\n");
        text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>Discord</gradient><reset>: <white>@marrineer\n");
        text.append("<gray>|<reset>\n");
        if (status.getStatus()) {
            text.append("<gray>|<reset> <b><gradient:#FFFFFF:#C81EEC>Status</gradient></b><reset>:\n");
            text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>Vault</gradient><reset>: ").append(ECONEnabled ? "<green>Enabled\n" : "<red>Disabled\n");
            text.append("<gray>|<reset>   <gradient:#FFFFFF:#1EA1EC>PlaceholderAPI</gradient><reset>: ").append(PAPIEnabled ? "<green>Enabled\n" : "<red>Disabled\n");
            text.append("<gray>|<reset>\n");
        }
        text.append("<dark_gray>[]=========================================[]</dark_gray>\n");

        Bukkit.getConsoleSender().sendMessage(
                MiniMessage.miniMessage().deserialize(text.toString())
        );
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
        return true;
    }

    // Instance
    public MessageManager getMessageManager() {
        return messageManager;
    }

    public FileConfiguration getMessage() {
        return messageManager.getMessages();
    }

    public Economy getEconomy() {
        return econ;
    }

    public Audience audience(CommandSender sender) {
        return adventure.sender(sender);
    }

    public Audience audience(Player player) {
        return adventure.player(player);
    }

    public boolean hasPAPI() {
        return PAPIEnabled;
    }
}
