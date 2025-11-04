package com.qhuy.coordLeak.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CoordLeakExpansion extends PlaceholderExpansion {
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
        return switch (identifier.toLowerCase()) {
            case "dimension" -> switch (player.getWorld().getEnvironment()) {
                case NORMAL -> "Overworld";
                case NETHER -> "Nether";
                case THE_END -> "The End";
                default -> player.getWorld().getName();
            };
            case "posx" -> String.valueOf(player.getLocation().getBlockX());
            case "posz" -> String.valueOf(player.getLocation().getBlockZ());
            default -> null;
        };
    }
}
