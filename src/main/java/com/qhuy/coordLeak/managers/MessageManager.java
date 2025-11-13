package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MessageManager {
    private final CoordLeak plugin;
    private final File file;
    private FileConfiguration messages;

    public MessageManager(CoordLeak plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        reloadMessage();
    }

    public void reloadMessage() {
        this.messages = YamlConfiguration.loadConfiguration(file);
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8)) {
            FileConfiguration defaultMessage = YamlConfiguration.loadConfiguration(reader);
            messages.setDefaults(defaultMessage);
            messages.options().copyDefaults(true);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load default messages.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public String getString(String key, String defaultValue) {
        if (messages == null) {
            plugin.getLogger().warning("messages.yml has not been loaded. Returning default value for key: " + key);
            return defaultValue;
        }
        String value = messages.getString(key);
        if (value == null) {
            plugin.getLogger().warning("Missing message key '" + key + "' in messages.yml. Using default value.");
            return defaultValue;
        }
        return value;
    }

    public List<String> getStringList(String key, List<String> defaultList) {
        if (messages == null) {
            plugin.getLogger().warning("messages.yml has not been loaded. Returning default list for key: " + key);
            return defaultList;
        }
        List<String> value = messages.getStringList(key);
        if (value == null || value.isEmpty()) {
            plugin.getLogger().warning("Missing message list key '" + key + "' in messages.yml or list is empty. Using default list.");
            return defaultList;
        }
        return value;
    }
}
