package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MessageManager {
    private final CoordLeak plugin;
    private File file;
    private FileConfiguration messages;

    public MessageManager(CoordLeak plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if(!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        reloadMessage();
    }
    public void reloadMessage() {
        this.messages = YamlConfiguration.loadConfiguration(file);
        FileConfiguration defaultMessage = YamlConfiguration.loadConfiguration(
                new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8)
        );
        messages.setDefaults(defaultMessage);
        messages.options().copyDefaults(true);
    }
}
