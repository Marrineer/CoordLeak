package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DatabaseManager {
    private Connection connection;
    private String dbType;
    private String sqliteFile;
    private String host, dbName, user, password;
    private int port;
    private final CoordLeak plugin;

    private final Map<UUID, Integer> usageCache = new ConcurrentHashMap<>();

    public DatabaseManager(CoordLeak plugin) {
        this.plugin = plugin;
        String type = plugin.getConfig().getString("database.type", "SQLITE");
        dbType = type.toUpperCase();

        if (dbType.equals("MYSQL")) {
            host = plugin.getConfig().getString("database.host", "localhost");
            port = plugin.getConfig().getInt("database.port", 3306);
            dbName = plugin.getConfig().getString("database.name", "coordleak_db");
            user = plugin.getConfig().getString("database.user", "root");
            password = plugin.getConfig().getString("database.password", "password");
        } else {
            sqliteFile = plugin.getConfig().getString("database.sqlite-file", "data.db");
        }
    }

    public void connect() throws SQLException {
        if (dbType.equals("SQLITE")) {
            String url = "jdbc:sqlite:plugins/CoordLeak/" + sqliteFile;
            connection = DriverManager.getConnection(url);
        } else if (dbType.equals("MYSQL")) {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&autoReconnect=true";
            connection = DriverManager.getConnection(url, user, password);
        }

        String createTable = "CREATE TABLE IF NOT EXISTS playerUsage (" +
                "player_uuid TEXT PRIMARY KEY, " +
                "usage_count INTEGER DEFAULT 0" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }
    }

    public void disconnect() {
        flushCacheToDatabase();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while disconnecting from the database.", e);
        }
    }

    public void flushCacheToDatabase() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, Integer> entry : usageCache.entrySet()) {
                UUID uuid = entry.getKey();
                int count = entry.getValue();
                String sql = "INSERT INTO playerUsage (player_uuid, usage_count) VALUES (?, ?) " +
                        "ON CONFLICT(player_uuid) DO UPDATE SET usage_count = ?;";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.setInt(2, count);
                    pstmt.setInt(3, count);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to flush usage for player " + uuid, e);
                }
            }
            plugin.getLogger().info("Flushed usage cache to database.");
        });
    }

    public void getUsage(UUID uuid, Consumer<Integer> callback) {
        if (usageCache.containsKey(uuid)) {
            callback.accept(usageCache.get(uuid));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int result = 0;
            String sql = "SELECT usage_count FROM playerUsage WHERE player_uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    result = rs.getInt("usage_count");
                    usageCache.put(uuid, result);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get usage count for player " + uuid, e);
            }

            int finalResult = result;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalResult));
        });
    }

    public void onUsage(UUID uuid) {
        if (usageCache.containsKey(uuid)) {
            usageCache.computeIfPresent(uuid, (k, v) -> Math.max(0, v - 1));
        } else {
            onUsageAsync(uuid);
        }
    }

    public void onUsageAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE playerUsage SET usage_count = usage_count - 1 WHERE player_uuid = ? AND usage_count > 0;";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update usage count for player " + uuid, e);
            }
        });
    }

    public void setUsage(UUID uuid, int count) {
        usageCache.put(uuid, count);
    }

    public void buyUsage(UUID uuid, int amount) {
        usageCache.merge(uuid, amount, Integer::sum);
    }
}
