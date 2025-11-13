package com.qhuy.coordLeak.utils;

import com.qhuy.coordLeak.CoordLeak;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CoordLeakExpansion extends PlaceholderExpansion {
    private final CoordLeak plugin;

    public CoordLeakExpansion(CoordLeak plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "coordleak";
    }

    @Override
    public @NotNull String getAuthor() {
        return "qhuy";
    }

    @Override
    public @NotNull String getVersion() {
        return "v1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        // %coordleak_cooldown_remaining_<command>%
        if (identifier.startsWith("cooldown_remaining_")) {
            String cmd = identifier.substring("cooldown_remaining_".length());
            long remaining = plugin.getProtectionManager().getRemainingCooldown(player.getUniqueId(), cmd);
            return String.valueOf(remaining / 1000); // seconds
        }

        // %coordleak_last_shared_<player>%
        if (identifier.startsWith("last_shared_")) {
            // Placeholder for tracking; not implemented yet
            return "N/A";
        }

        return switch (identifier.toLowerCase()) {
            case "dimension" -> switch (player.getWorld().getEnvironment()) {
                case NORMAL -> "Overworld";
                case NETHER -> "Nether";
                case THE_END -> "The End";
                default -> player.getWorld().getName();
            };
            case "posx" -> String.valueOf(player.getLocation().getBlockX());
            case "posy" -> String.valueOf(player.getLocation().getBlockY());
            case "posz" -> String.valueOf(player.getLocation().getBlockZ());
            default -> null;
        };
    }
}
