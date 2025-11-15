package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLoggerTest {

    @Mock
    private CoordLeak plugin;

    @Mock
    private ConfigManager configManager;

    private File tempDir;
    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "coordleak-audit-test-" + System.currentTimeMillis());
        tempDir.mkdirs();

        lenient().when(plugin.getDataFolder()).thenReturn(tempDir);
        lenient().when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("Test"));
        lenient().when(configManager.isAuditLoggingEnabled()).thenReturn(true);
        lenient().when(configManager.getAuditLogFile()).thenReturn("logs/coordleak.log");
        lenient().when(configManager.isIpLoggingEnabled()).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        if (auditLogger != null) {
            auditLogger.shutdown();
        }
        deleteDirectory(tempDir);
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    @Test
    void testLog_AuditingEnabled() throws InterruptedException {
        auditLogger = new AuditLogger(plugin, configManager);
        auditLogger.log("TEST_ACTION", "executor", "target", 100.0, "SUCCESS", "192.168.1.1", "Test details");

        Thread.sleep(200);

        File logsDir = new File(tempDir, "logs");
        assertTrue(logsDir.exists());
        File[] logFiles = logsDir.listFiles((dir, name) -> name.startsWith("coordleak") && name.endsWith(".log"));
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 0);
    }

    @Test
    void testLog_AuditingDisabled() throws InterruptedException {
        when(configManager.isAuditLoggingEnabled()).thenReturn(false);
        auditLogger = new AuditLogger(plugin, configManager);
        auditLogger.log("TEST_ACTION", "executor", "target", 100.0, "SUCCESS", "192.168.1.1", "Test details");

        Thread.sleep(200);

        File logsDir = new File(tempDir, "logs");
        if (logsDir.exists()) {
            File[] logFiles = logsDir.listFiles();
            assertTrue(logFiles == null || logFiles.length == 0);
        }
    }

    @Test
    void testLog_IpRedaction() throws InterruptedException, IOException {
        auditLogger = new AuditLogger(plugin, configManager);
        auditLogger.log("LEAK", "player1", "player2", 50.0, "SUCCESS", "192.168.1.1", "Coordinates leaked");

        Thread.sleep(200);

        File logsDir = new File(tempDir, "logs");
        File[] logFiles = logsDir.listFiles((dir, name) -> name.startsWith("coordleak") && name.endsWith(".log"));
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 0);

        String content = Files.readString(logFiles[0].toPath());
        assertTrue(content.contains("\"ip\":\"N/A\""));
        assertFalse(content.contains("192.168.1.1"));
    }

    @Test
    void testLog_IpLoggingEnabled() throws InterruptedException, IOException {
        when(configManager.isIpLoggingEnabled()).thenReturn(true);
        auditLogger = new AuditLogger(plugin, configManager);
        auditLogger.log("LEAK", "player1", "player2", 50.0, "SUCCESS", "192.168.1.1", "Coordinates leaked");

        Thread.sleep(200);

        File logsDir = new File(tempDir, "logs");
        File[] logFiles = logsDir.listFiles((dir, name) -> name.startsWith("coordleak") && name.endsWith(".log"));
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 0);

        String content = Files.readString(logFiles[0].toPath());
        assertTrue(content.contains("192.168.1.1"));
    }

    @Test
    void testLog_JsonFormat() throws InterruptedException, IOException {
        auditLogger = new AuditLogger(plugin, configManager);
        auditLogger.log("SHARE", "sender", "receiver", 0.0, "SUCCESS", "10.0.0.1", "Shared coordinates");

        Thread.sleep(200);

        File logsDir = new File(tempDir, "logs");
        File[] logFiles = logsDir.listFiles((dir, name) -> name.startsWith("coordleak") && name.endsWith(".log"));
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 0);

        String content = Files.readString(logFiles[0].toPath());
        assertTrue(content.contains("\"action\":\"SHARE\""));
        assertTrue(content.contains("\"executor\":\"sender\""));
        assertTrue(content.contains("\"target\":\"receiver\""));
        assertTrue(content.contains("\"result\":\"SUCCESS\""));
    }

    @Test
    void testLog_EscapesSpecialCharacters() throws InterruptedException, IOException {
        auditLogger = new AuditLogger(plugin, configManager);
        auditLogger.log("TEST", "user\"test", "target\\test", 0.0, "SUCCESS", "N/A", "Details\nwith\nnewlines");

        Thread.sleep(200);

        File logsDir = new File(tempDir, "logs");
        File[] logFiles = logsDir.listFiles((dir, name) -> name.startsWith("coordleak") && name.endsWith(".log"));
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 0);

        String content = Files.readString(logFiles[0].toPath());
        assertTrue(content.contains("\\\""));
        assertTrue(content.contains("\\\\"));
        assertTrue(content.contains("\\n"));
    }

    @Test
    void testShutdown_CompletesGracefully() {
        auditLogger = new AuditLogger(plugin, configManager);
        assertDoesNotThrow(() -> auditLogger.shutdown());
    }
}
