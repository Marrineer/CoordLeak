package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final CoordLeak plugin;
    private double price;
    private long cooldown;

    public ConfigManager(CoordLeak plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        price = config.getDouble("price", 500.0);
        cooldown = config.getLong("settings.cooldown-per-usage", 300);
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        plugin.getConfig().set("price", price);
        plugin.saveConfig();
    }

    public long getCooldown() {
        return cooldown;
    }
}
