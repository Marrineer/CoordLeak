package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DatabaseManager {
    private Connection connection;
    private final CoordLeak plugin;

    public DatabaseManager(CoordLeak plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        String url = "jdbc:sqlite:plugins/CoordLeak/data.db";
        connection = DriverManager.getConnection(url);

        String createTable = "CREATE TABLE IF NOT EXISTS playerUsage (" +
                "player_uuid TEXT PRIMARY KEY, " +
                "usage_count INTEGER DEFAULT 0" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }
    }
    public void disconnect(CoordLeak plugin) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while disconnecting from the database.", e);
        }
    }

    public void getUsageCountAsync(UUID playerUUID, CoordLeak plugin, Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int result = 0;
            String sql = "SELECT usage_count FROM playerUsage WHERE player_uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    result = rs.getInt("usage_count");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get usage count for player " + playerUUID, e);
            }

            int finalResult = result;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalResult));
        });
    }
    public void addUsageCountAsync(UUID playerUUID, CoordLeak plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO playerUsage (player_uuid, usage_count) VALUES (?, 1) " +
                    "ON CONFLICT(player_uuid) DO UPDATE SET usage_count = usage_count + 1;";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add usage count for player " + playerUUID, e);
            }
        });
    }
    public void setUsageCountAsync(UUID playerUUID, CoordLeak plugin, int count) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO playerUsage (player_uuid, usage_count) VALUES (?, ?) " +
                    "ON CONFLICT(player_uuid) DO UPDATE SET usage_count = ?;";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setInt(2, count);
                pstmt.setInt(3, count);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set usage count for player " + playerUUID, e);
            }
        });
    }
    public void onUsageAsync(UUID playerUUID, CoordLeak plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE playerUsage SET usage_count = usage_count - 1 WHERE player_uuid = ? AND usage_count > 0;";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update usage count for player " + playerUUID, e);
            }
        });
    }
}