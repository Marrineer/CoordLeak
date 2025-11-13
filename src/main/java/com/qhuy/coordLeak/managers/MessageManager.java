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
        return defaultList; // Return defaultList if value is null or empty
    }

    // --- New Message Getters ---
    public String getPrefix() {
        return getString("prefix", "&7[&bCoordLeak&7] ");
    }

    public String getNoPermission() {
        return getString("no-permission", "&cYou don't have permission to do that.");
    }

    public String getPlayerOnlyCommand() {
        return getString("player-only-command", "&cThis command can only be run by a player.");
    }

    public String getCommandCooldown() {
        return getString("command-cooldown", "&cYou are on cooldown for this command. Please wait %time%.");
    }

    public String getCommandRateLimited() {
        return getString("command-rate-limited", "&cYou are sending commands too fast. Please slow down.");
    }

    public String getGlobalRateLimited() {
        return getString("global-rate-limited", "&cThe server is currently under high load. Please try again in a moment.");
    }

    public String getDailyLimitExceeded() {
        return getString("daily-limit-exceeded", "&cYou have exceeded your daily limit for this command.");
    }

    public String getPlayerBlacklisted() {
        return getString("player-blacklisted", "&cYou are blacklisted from using this feature.");
    }

    public String getPlayerNotWhitelisted() {
        return getString("player-not-whitelisted", "&cYou are not whitelisted to use this feature.");
    }

    public String getInvalidPrice() {
        return getString("invalid-price", "&cInvalid price. Please enter a valid number.");
    }

    public String getPriceOutOfRange() {
        return getString("price-out-of-range", "&cThe price must be between %min_price% and %max_price%.");
    }

    public String getEconomyError() {
        return getString("economy-error", "&cAn economy error occurred. Please contact an administrator.");
    }

    public String getInsufficientFunds() {
        return getString("insufficient-funds", "&cYou do not have enough money to do that. You need %amount%.");
    }

    public String getTargetNotOnline() {
        return getString("target-not-online", "&cThe target player is not online.");
    }

    public String getTargetExcludedWorld() {
        return getString("target-excluded-world", "&cYou cannot leak coordinates in this world.");
    }

    public String getTargetExcludedPermission() {
        return getString("target-excluded-permission", "&cThis player is protected from coordinate leaks.");
    }

    public String getTargetNoConsent() {
        return getString("target-no-consent", "&cThe target player has not consented to share their coordinates.");
    }

    public String getReloadConfirmRequired() {
        return getString("reload-confirm-required", "&ePlease confirm your reload by typing &6/coord reload confirm &ewithin %time% seconds.");
    }

    public String getReloadConfirmed() {
        return getString("reload-confirmed", "&aPlugin reloaded successfully!");
    }

    public String getReloadCancelled() {
        return getString("reload-cancelled", "&cReload confirmation expired or cancelled.");
    }

    public String getSetPriceSuccess() {
        return getString("setprice-success", "&aPrice for leaking coordinates set to &e%price%&a.");
    }

    public String getLeakSuccess() {
        return getString("leak-success", "&aYou successfully leaked &e%player%'s &acoordinates: &e%coords%&a.");
    }

    public String getShareSuccessSender() {
        return getString("share-success-sender", "&aYou shared &e%player%'s &acoordinates with &e%target%&a.");
    }

    public String getShareSuccessTarget() {
        return getString("share-success-target", "&a%sender% &ashared &e%player%'s &acoordinates with you: &e%coords%&a.");
    }

    public String getNoLeakTargetFound() {
        return getString("no-leak-target-found", "&cNo suitable player found to leak coordinates.");
    }

    public String getAdminActionLogged() {
        return getString("admin-action-logged", "&eAdmin action logged.");
    }

    public String getGlobalLimitExceededLog() {
        return getString("global-limit-exceeded-log", "&c[CoordLeak] Global rate limit exceeded. Blocking requests for %time% seconds.");
    }
}
