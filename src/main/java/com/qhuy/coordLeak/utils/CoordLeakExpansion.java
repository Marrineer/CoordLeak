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
        // Null safety
        if (player == null) {
            return "";
        }
        
        // Lifecycle safety - check if plugin/managers are still valid
        if (plugin == null || plugin.getProtectionManager() == null) {
            return "";
        }

        // %coordleak_cooldown_remaining_<command>%
        if (identifier.startsWith("cooldown_remaining_")) {
            String cmd = identifier.substring("cooldown_remaining_".length());
            
            // Validate command name
            if (cmd.isEmpty() || !cmd.matches("[a-z]+")) {
                return "0";
            }
            
            try {
                long remaining = plugin.getProtectionManager().getRemainingCooldown(player.getUniqueId(), cmd);
                return String.valueOf(remaining / 1000); // seconds
            } catch (Exception e) {
                return "0";
            }
        }

        // %coordleak_last_shared_<player>%
        if (identifier.startsWith("last_shared_")) {
            // Not implemented - return empty instead of "N/A"
            return "";
        }

        return switch (identifier.toLowerCase()) {
            case "dimension" -> {
                try {
                    if (player.getWorld() == null) yield "Unknown";
                    yield switch (player.getWorld().getEnvironment()) {
                        case NORMAL -> "Overworld";
                        case NETHER -> "Nether";
                        case THE_END -> "The End";
                        default -> player.getWorld().getName();
                    };
                } catch (Exception e) {
                    yield "Unknown";
                }
            }
            case "posx" -> {
                try {
                    yield String.valueOf(player.getLocation().getBlockX());
                } catch (Exception e) {
                    yield "0";
                }
            }
            case "posy" -> {
                try {
                    yield String.valueOf(player.getLocation().getBlockY());
                } catch (Exception e) {
                    yield "0";
                }
            }
            case "posz" -> {
                try {
                    yield String.valueOf(player.getLocation().getBlockZ());
                } catch (Exception e) {
                    yield "0";
                }
            }
            case "price" -> {
                try {
                    if (plugin.getConfigManager() == null) yield "0.00";
                    yield String.format("%.2f", plugin.getConfigManager().getDefaultPrice());
                } catch (Exception e) {
                    yield "0.00";
                }
            }
            default -> null;
        };
    }
}
