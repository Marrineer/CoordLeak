package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
        reloadMessages();
    }

    public void reloadMessages() {
        this.messages = YamlConfiguration.loadConfiguration(file);
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8)) {
            FileConfiguration defaultMessages = YamlConfiguration.loadConfiguration(reader);
            messages.setDefaults(defaultMessages);
            messages.options().copyDefaults(true);
            plugin.saveResource("messages.yml", false); // Save to copy new defaults
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load default messages.yml: " + e.getMessage());
        }
    }

    /**
     * Gets a raw string from the messages file.
     *
     * @param key          The key of the message.
     * @param defaultValue The default value if the key is not found.
     * @return The raw string.
     */
    public String getString(String key, String defaultValue) {
        return messages.getString(key, defaultValue);
    }

    /**
     * Gets a raw list of strings from the messages file.
     *
     * @param key The key of the message list.
     * @return The raw list of strings.
     */
    public List<String> getStringList(String key) {
        return messages.getStringList(key);
    }

    /**
     * Formats a message by replacing placeholders.
     *
     * @param message      The message to format.
     * @param player       The player for PlaceholderAPI, can be null.
     * @param replacements Placeholders and their values, e.g., "%placeholder%", "value".
     * @return The formatted message string.
     */
    private String format(String message, Player player, String... replacements) {
        message = message.replace("{prefix}", plugin.getConfigManager().getPrefix());

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        if (plugin.hasPAPI() && player != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        return message;
    }

    /**
     * Gets a formatted string from messages.yml.
     *
     * @param key          The key of the message.
     * @param player       The player for PAPI placeholders, can be null.
     * @param replacements Placeholders and their values.
     * @return The formatted string.
     */
    public String getFormattedString(String key, Player player, String... replacements) {
        String message = getString(key, "<red>Missing message key: " + key + "</red>");
        return format(message, player, replacements);
    }

    /**
     * Sends a formatted message to a CommandSender.
     *
     * @param sender       The recipient of the message.
     * @param key          The key of the message in messages.yml.
     * @param replacements Placeholders and their values.
     */
    public void send(CommandSender sender, String key, String... replacements) {
        Player player = sender instanceof Player ? (Player) sender : null;
        String message = getFormattedString(key, player, replacements);
        plugin.audience(sender).sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    /**
     * Sends a list of formatted messages to a CommandSender.
     *
     * @param sender       The recipient of the messages.
     * @param key          The key of the message list in messages.yml.
     * @param replacements Placeholders and their values.
     */
    public void sendList(CommandSender sender, String key, String... replacements) {
        Player player = sender instanceof Player ? (Player) sender : null;
        List<String> messageList = getStringList(key);

        if (messageList.isEmpty()) {
            send(sender, "config-error");
            return;
        }

        for (String message : messageList) {
            String formattedMessage = format(message, player, replacements);
            plugin.audience(sender).sendMessage(MiniMessage.miniMessage().deserialize(formattedMessage));
        }
    }
}
