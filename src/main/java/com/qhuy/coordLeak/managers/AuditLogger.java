package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class AuditLogger {

    private final CoordLeak plugin;
    private final ConfigManager configManager;
    private final ExecutorService executor;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public AuditLogger(CoordLeak plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.executor = Executors.newSingleThreadExecutor(); // Use a single thread for sequential logging
    }

    public void log(String action, String executorName, String target, double price, String result, String ipAddress, String details) {
        if (!configManager.isAuditLoggingEnabled()) {
            return;
        }

        executor.submit(() -> {
            try {
                String logFileName = getLogFileName();
                Path logFilePath = Paths.get(plugin.getDataFolder().getAbsolutePath(), logFileName);
                File logFile = logFilePath.toFile();

                // Ensure directory exists
                Files.createDirectories(logFilePath.getParent());

                // Check for rolling file (daily)
                if (!logFile.exists() || !fileDateFormat.format(new Date(logFile.lastModified())).equals(fileDateFormat.format(new Date()))) {
                    // If file doesn't exist or is from a different day, create a new one
                    if (logFile.exists()) {
                        // Optionally rename old file with timestamp if needed, but for daily rolling, just new file is fine
                    }
                    Files.createFile(logFilePath);
                }

                String logEntry = String.format("[%s] ACTION: %s | EXECUTOR: %s | TARGET: %s | PRICE: %.2f | RESULT: %s | IP: %s | DETAILS: %s%n",
                        dateFormat.format(new Date()), action, executorName, target, price, result,
                        configManager.isAuditLogSensitiveEnabled() ? ipAddress : "N/A", details);

                Files.write(logFilePath, logEntry.getBytes(), StandardOpenOption.APPEND);

            } catch (IOException e) {
                plugin.getLogger().severe("Failed to write to audit log: " + e.getMessage());
            }
        });
    }

    private String getLogFileName() {
        String configuredFileName = configManager.getAuditLogFile();
        if (configuredFileName == null || configuredFileName.isEmpty()) {
            configuredFileName = "logs/coordleak.log"; // Default if not configured
        }

        // Ensure it's relative to data folder and includes date for rolling
        String baseName = configuredFileName;
        String extension = "";
        int dotIndex = configuredFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = configuredFileName.substring(0, dotIndex);
            extension = configuredFileName.substring(dotIndex);
        }

        return baseName + "_" + fileDateFormat.format(new Date()) + extension;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
