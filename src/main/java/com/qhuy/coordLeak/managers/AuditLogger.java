package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AuditLogger {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int DEFAULT_RETENTION_DAYS = 30;
    private final CoordLeak plugin;
    private final ConfigManager configManager;
    private final ExecutorService executor;
    private final LinkedBlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final DateTimeFormatter iso8601 = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());

    public AuditLogger(CoordLeak plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.executor = Executors.newSingleThreadExecutor();
        startLogWorker();
    }

    private void startLogWorker() {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String entry = logQueue.take();
                    writeToFile(entry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    plugin.getLogger().severe("Audit log worker error: " + e.getMessage());
                }
            }
        });
    }

    public void log(String action, String executorName, String target, double price, String result, String ipAddress, String details) {
        if (!configManager.isAuditLoggingEnabled()) {
            return;
        }

        String timestamp = iso8601.format(Instant.now());
        String logEntry = String.format("{\"timestamp\":\"%s\",\"action\":\"%s\",\"executor\":\"%s\",\"target\":\"%s\",\"price\":%.2f,\"result\":\"%s\",\"ip\":\"%s\",\"details\":\"%s\"}%n",
                timestamp, escape(action), escape(executorName), escape(target), price, escape(result),
                configManager.isIpLoggingEnabled() ? escape(ipAddress) : "N/A", escape(details));

        logQueue.offer(logEntry);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void writeToFile(String entry) throws IOException {
        String logFileName = getLogFileName();
        Path logFilePath = Paths.get(plugin.getDataFolder().getAbsolutePath(), logFileName);
        File logFile = logFilePath.toFile();

        Files.createDirectories(logFilePath.getParent());

        if (!logFile.exists()) {
            Files.createFile(logFilePath);
        }

        // Check file size rotation
        if (logFile.length() > MAX_FILE_SIZE) {
            rotateLogFile(logFile);
        }

        Files.write(logFilePath, entry.getBytes(), StandardOpenOption.APPEND);

        // Clean old logs
        cleanOldLogs();
    }

    private void rotateLogFile(File current) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File rotated = new File(current.getParent(), current.getName() + "." + timestamp);
        Files.move(current.toPath(), rotated.toPath());
    }

    private void cleanOldLogs() {
        try {
            File logsDir = new File(plugin.getDataFolder(), "logs");
            if (!logsDir.exists()) return;

            long cutoff = System.currentTimeMillis() - (DEFAULT_RETENTION_DAYS * 24L * 60 * 60 * 1000);
            File[] files = logsDir.listFiles((dir, name) -> name.startsWith("coordleak") && name.endsWith(".log"));

            if (files != null) {
                for (File f : files) {
                    if (f.lastModified() < cutoff) {
                        f.delete();
                        plugin.getLogger().info("Deleted old audit log: " + f.getName());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clean old logs: " + e.getMessage());
        }
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
