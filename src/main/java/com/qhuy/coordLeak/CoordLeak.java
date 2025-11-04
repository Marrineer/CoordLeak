package com.qhuy.coordLeak;

import com.qhuy.coordLeak.commands.CoordCommand;
import com.qhuy.coordLeak.managers.CooldownManager;
import com.qhuy.coordLeak.managers.MessageManager;
import com.qhuy.coordLeak.utils.CoordLeakExpansion;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
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
        info("Enabling");
    }

    @Override
    public void onDisable() {
        info("Disabling");
        if (PAPI != null) {
            PAPI.unregister();
        }
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        saveConfig();
    }

    private void info(String msg) {
        StringBuilder text = new StringBuilder("\n\n");
        text.append("&8[]===========[").append(msg).append(" &cCoordLeak&8]===========[]\n");
        text.append("&8|\n");
        text.append("&8| &cInformation:\n");
        text.append("&8|\n");
        text.append("&8|   &9Name: &bCoordLeak\n");
        text.append("&8|   &9Author: ").append(getDescription().getAuthors()).append("\n");
        text.append("&8|\n");
        text.append("&8| &9Contact:\n");
        text.append("&8|   &9Email: &bmarrineer@gmail.com\n");
        text.append("&8|   &9Discord: &b@marrineer\n");
        text.append("&8|\n");
        text.append("&8| &9Status:\n");
        text.append("&8|   &9Vault: \n").append(ECONEnabled ? "&aEnabled\n" : "&cDisabled\n");
        text.append("&8|   &9PlaceholderAPI: ").append(PAPIEnabled ? "&aEnabled\n" : "&cDisabled\n");
        text.append("&8|\n");
        text.append("&8[]=========================================[]\n");

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', text.toString()));
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
