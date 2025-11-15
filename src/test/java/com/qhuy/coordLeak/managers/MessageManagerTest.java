package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageManagerTest {

    @Mock
    private CoordLeak plugin;

    @Mock
    private ConfigManager configManager;

    @Mock
    private BukkitAudiences adventure;

    @Mock
    private Audience audience;

    @Mock
    private CommandSender sender;

    @Mock
    private Player player;

    private File tempDir;
    private File messagesFile;
    private MessageManager messageManager;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "coordleak-test-" + System.currentTimeMillis());
        tempDir.mkdirs();
        messagesFile = new File(tempDir, "messages.yml");

        String defaultMessages = """
                test-message: "<green>Test message</green>"
                test-with-placeholder: "Hello %player%"
                test-list:
                  - "Line 1"
                  - "Line 2"
                  - "Line 3"
                no-permission: "no-permission"
                player-only-command: "player-only-command"
                config-error: "config-error"
                command-cooldown: "command-cooldown"
                command-rate-limited: "command-rate-limited"
                daily-limit-exceeded: "daily-limit-exceeded"
                economy-error: "economy-error"
                insufficient-funds: "insufficient-funds"
                leak-success: "leak-success"
                share-success-sender: "share-success-sender"
                share-success-target: "share-success-target"
                reload-confirmed: "reload-confirmed"
                setprice-success: "setprice-success"
                """;
        try (FileWriter writer = new FileWriter(messagesFile)) {
            writer.write(defaultMessages);
        }

        lenient().when(plugin.getDataFolder()).thenReturn(tempDir);
        lenient().when(plugin.getConfigManager()).thenReturn(configManager);
        lenient().when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("Test"));
        lenient().when(plugin.hasPAPI()).thenReturn(false);
        lenient().when(plugin.audience(any(CommandSender.class))).thenReturn(audience);
        lenient().when(configManager.getPrefix()).thenReturn("[Test]");

        InputStream resourceStream = new ByteArrayInputStream(defaultMessages.getBytes(StandardCharsets.UTF_8));
        lenient().when(plugin.getResource("messages.yml")).thenReturn(resourceStream);
    }

    @Test
    void testGetString_ExistingKey() {
        messageManager = new MessageManager(plugin);
        String message = messageManager.getString("test-message", "default");
        assertEquals("<green>Test message</green>", message);
    }

    @Test
    void testGetString_MissingKey() {
        messageManager = new MessageManager(plugin);
        String message = messageManager.getString("non-existent", "default value");
        assertEquals("default value", message);
    }

    @Test
    void testGetStringList_ExistingKey() {
        messageManager = new MessageManager(plugin);
        List<String> messages = messageManager.getStringList("test-list");
        assertEquals(3, messages.size());
        assertEquals("Line 1", messages.get(0));
    }

    @Test
    void testGetStringList_MissingKey() {
        messageManager = new MessageManager(plugin);
        List<String> messages = messageManager.getStringList("non-existent");
        assertTrue(messages.isEmpty());
    }

    @Test
    void testGetFormattedString_WithPlaceholders() {
        messageManager = new MessageManager(plugin);
        String formatted = messageManager.getFormattedString("test-with-placeholder", null, "%player%", "TestPlayer");
        assertTrue(formatted.contains("TestPlayer"));
    }

    @Test
    void testGetFormattedString_WithPrefix() {
        messageManager = new MessageManager(plugin);
        String messageContent = "{prefix} Test";
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
        config.set("prefix-test", messageContent);
        try {
            config.save(messagesFile);
        } catch (IOException e) {
            fail("Failed to save config");
        }
        
        messageManager.reloadMessages();
        String formatted = messageManager.getFormattedString("prefix-test", null);
        assertTrue(formatted.contains("[Test]"));
    }

    @Test
    void testSend_ToCommandSender() {
        messageManager = new MessageManager(plugin);
        assertDoesNotThrow(() -> messageManager.send(sender, "test-message"));
        verify(plugin).audience(sender);
    }

    @Test
    void testSend_WithReplacements() {
        messageManager = new MessageManager(plugin);
        assertDoesNotThrow(() -> messageManager.send(sender, "test-with-placeholder", "%player%", "Alice"));
        verify(plugin).audience(sender);
    }

    @Test
    void testSendList_ValidKey() {
        messageManager = new MessageManager(plugin);
        assertDoesNotThrow(() -> messageManager.sendList(sender, "test-list"));
        verify(audience, times(3)).sendMessage(any());
    }

    @Test
    void testReloadMessages() {
        messageManager = new MessageManager(plugin);
        assertDoesNotThrow(() -> messageManager.reloadMessages());
    }
}
