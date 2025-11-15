package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProtectionManagerTest {

    @Mock
    private CoordLeak plugin;

    @Mock
    private ConfigManager configManager;

    @Mock
    private Player player;

    @Mock
    private World world;

    @Mock
    private java.util.logging.Logger logger;

    private ProtectionManager protectionManager;
    private UUID playerUUID;

    @BeforeEach
    void setUp() {
        playerUUID = UUID.randomUUID();
        protectionManager = new ProtectionManager(plugin, configManager);

        lenient().when(plugin.getLogger()).thenReturn(logger);
        lenient().when(player.getUniqueId()).thenReturn(playerUUID);
        lenient().when(player.hasPermission(anyString())).thenReturn(false);
        lenient().when(player.isOnline()).thenReturn(true);
        lenient().when(player.getWorld()).thenReturn(world);
        lenient().when(world.getName()).thenReturn("world");
    }

    @Test
    void testCooldown_NotOnCooldown() {
        lenient().when(configManager.getCooldown("leak")).thenReturn(5000L);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(null);
            assertFalse(protectionManager.isOnCooldown(playerUUID, "leak"));
        }
    }

    @Test
    void testCooldown_ApplyAndCheck() {
        lenient().when(configManager.getCooldown("leak")).thenReturn(5000L);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(null);

            protectionManager.applyCooldown(playerUUID, "leak");
            assertTrue(protectionManager.isOnCooldown(playerUUID, "leak"));
            assertTrue(protectionManager.getRemainingCooldown(playerUUID, "leak") > 0);
        }
    }

    @Test
    void testCooldown_BypassWithPermission() {
        lenient().when(configManager.getCooldown("leak")).thenReturn(5000L);
        when(player.hasPermission("coordleak.bypass.cooldown")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);

            protectionManager.applyCooldown(playerUUID, "leak");
            assertFalse(protectionManager.isOnCooldown(playerUUID, "leak"));
            assertEquals(0, protectionManager.getRemainingCooldown(playerUUID, "leak"));
        }
    }

    @Test
    void testCooldown_AdminBypassAll() {
        lenient().when(configManager.getCooldown("leak")).thenReturn(5000L);
        when(player.hasPermission("coordleak.admin")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);

            protectionManager.applyCooldown(playerUUID, "leak");
            assertFalse(protectionManager.isOnCooldown(playerUUID, "leak"));
        }
    }

    @Test
    void testRateLimit_WithinLimit() {
        when(configManager.getRateLimit("leak")).thenReturn(3);
        when(configManager.getRateLimitWindow("leak")).thenReturn(1000L);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(null);

            assertFalse(protectionManager.isRateLimited(playerUUID, "leak"));
            protectionManager.recordRateLimitUsage(playerUUID, "leak");
            assertFalse(protectionManager.isRateLimited(playerUUID, "leak"));
        }
    }

    @Test
    void testRateLimit_ExceedsLimit() {
        when(configManager.getRateLimit("leak")).thenReturn(3);
        when(configManager.getRateLimitWindow("leak")).thenReturn(60000L);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(null);

            for (int i = 0; i < 3; i++) {
                assertFalse(protectionManager.isRateLimited(playerUUID, "leak"));
                protectionManager.recordRateLimitUsage(playerUUID, "leak");
            }
            assertTrue(protectionManager.isRateLimited(playerUUID, "leak"));
        }
    }

    @Test
    void testRateLimit_BypassWithPermission() {
        lenient().when(configManager.getRateLimit("leak")).thenReturn(1);
        lenient().when(configManager.getRateLimitWindow("leak")).thenReturn(60000L);
        when(player.hasPermission("coordleak.bypass.ratelimit")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);

            for (int i = 0; i < 5; i++) {
                protectionManager.recordRateLimitUsage(playerUUID, "leak");
            }
            assertFalse(protectionManager.isRateLimited(playerUUID, "leak"));
        }
    }

    @Test
    void testDailyLimit_WithinLimit() {
        when(configManager.getDailyLimit("leak")).thenReturn(5);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(null);

            assertFalse(protectionManager.hasExceededDailyLimit(playerUUID, "leak"));
            protectionManager.incrementDailyUsage(playerUUID, "leak");
            assertFalse(protectionManager.hasExceededDailyLimit(playerUUID, "leak"));
        }
    }

    @Test
    void testDailyLimit_ExceedsLimit() {
        when(configManager.getDailyLimit("leak")).thenReturn(3);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(null);

            for (int i = 0; i < 3; i++) {
                protectionManager.incrementDailyUsage(playerUUID, "leak");
            }
            assertTrue(protectionManager.hasExceededDailyLimit(playerUUID, "leak"));
        }
    }

    @Test
    void testDailyLimit_BypassWithPermission() {
        lenient().when(configManager.getDailyLimit("leak")).thenReturn(1);
        when(player.hasPermission("coordleak.bypass.dailylimit")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);

            for (int i = 0; i < 10; i++) {
                protectionManager.incrementDailyUsage(playerUUID, "leak");
            }
            assertFalse(protectionManager.hasExceededDailyLimit(playerUUID, "leak"));
        }
    }

    @Test
    void testBlacklist_PlayerBlacklisted() {
        when(configManager.getBlacklistedUUIDs()).thenReturn(Arrays.asList(playerUUID.toString()));
        assertTrue(protectionManager.isBlacklisted(playerUUID));
    }

    @Test
    void testBlacklist_PlayerNotBlacklisted() {
        when(configManager.getBlacklistedUUIDs()).thenReturn(Arrays.asList());
        assertFalse(protectionManager.isBlacklisted(playerUUID));
    }

    @Test
    void testWhitelist_DisabledAllowsEveryone() {
        when(configManager.isWhitelistEnabled()).thenReturn(false);
        assertTrue(protectionManager.isWhitelisted(playerUUID));
    }

    @Test
    void testWhitelist_EnabledAndPlayerInList() {
        when(configManager.isWhitelistEnabled()).thenReturn(true);
        when(configManager.getWhitelistedUUIDs()).thenReturn(Arrays.asList(playerUUID.toString()));
        assertTrue(protectionManager.isWhitelisted(playerUUID));
    }

    @Test
    void testWhitelist_EnabledAndPlayerNotInList() {
        when(configManager.isWhitelistEnabled()).thenReturn(true);
        when(configManager.getWhitelistedUUIDs()).thenReturn(Arrays.asList());
        assertFalse(protectionManager.isWhitelisted(playerUUID));
    }

    @Test
    void testValidTarget_OnlineAndNoExclusions() {
        when(configManager.getExcludedWorlds()).thenReturn(Arrays.asList());
        when(configManager.getExcludedPermissions()).thenReturn(Arrays.asList());

        assertTrue(protectionManager.isValidTarget(player));
    }

    @Test
    void testValidTarget_OfflinePlayer() {
        when(player.isOnline()).thenReturn(false);
        assertFalse(protectionManager.isValidTarget(player));
    }

    @Test
    void testValidTarget_NullPlayer() {
        assertFalse(protectionManager.isValidTarget(null));
    }

    @Test
    void testValidTarget_InExcludedWorld() {
        when(configManager.getExcludedWorlds()).thenReturn(Arrays.asList("world"));

        assertFalse(protectionManager.isValidTarget(player));
    }

    @Test
    void testValidTarget_HasExcludedPermission() {
        when(configManager.getExcludedWorlds()).thenReturn(Arrays.asList());
        when(configManager.getExcludedPermissions()).thenReturn(Arrays.asList("coordleak.exclude"));
        when(player.hasPermission("coordleak.exclude")).thenReturn(true);

        assertFalse(protectionManager.isValidTarget(player));
    }

    @Test
    void testConfirmation_NoPendingConfirmation() {
        assertFalse(protectionManager.hasPendingConfirmation(playerUUID, "reload"));
    }

    @Test
    void testConfirmation_InitiateAndCheck() {
        lenient().when(configManager.getConfirmationTimeout("reload")).thenReturn(10000L);

        protectionManager.initiateConfirmation(playerUUID, "reload");
        assertTrue(protectionManager.hasPendingConfirmation(playerUUID, "reload"));
    }

    @Test
    void testConfirmation_ClearConfirmation() {
        lenient().when(configManager.getConfirmationTimeout("reload")).thenReturn(10000L);

        protectionManager.initiateConfirmation(playerUUID, "reload");
        protectionManager.clearConfirmation(playerUUID, "reload");
        assertFalse(protectionManager.hasPendingConfirmation(playerUUID, "reload"));
    }

    @Test
    void testGlobalRateLimit_WithinLimit() {
        lenient().when(configManager.getGlobalRateLimit()).thenReturn(100);
        lenient().when(configManager.getGlobalRateLimitWindow()).thenReturn(60000L);
        lenient().when(configManager.getGlobalRateLimitBlockDuration()).thenReturn(10000L);

        for (int i = 0; i < 50; i++) {
            assertFalse(protectionManager.isGlobalRateLimited());
        }
    }

    @Test
    void testGlobalRateLimit_ExceedsLimit() {
        lenient().when(configManager.getGlobalRateLimit()).thenReturn(5);
        lenient().when(configManager.getGlobalRateLimitWindow()).thenReturn(60000L);
        lenient().when(configManager.getGlobalRateLimitBlockDuration()).thenReturn(10000L);

        for (int i = 0; i <= 5; i++) {
            protectionManager.isGlobalRateLimited();
        }
        assertTrue(protectionManager.isGlobalRateLimited());
        assertTrue(protectionManager.getGlobalBlockRemaining() > 0);
    }

    @Test
    void testTargetConsent_ConsentNotRequired() {
        when(configManager.isConsentRequiredForShare()).thenReturn(false);
        assertTrue(protectionManager.hasTargetConsent(playerUUID));
    }

    @Test
    void testTargetConsent_ConsentRequired() {
        when(configManager.isConsentRequiredForShare()).thenReturn(true);
        assertFalse(protectionManager.hasTargetConsent(playerUUID));
    }

    @Test
    void testCleanup_RemovesExpiredEntries() {
        lenient().when(configManager.getCooldown("leak")).thenReturn(1L);
        lenient().when(configManager.getCooldownMillis("leak")).thenReturn(1L);
        lenient().when(configManager.getRateLimit("leak")).thenReturn(5);
        lenient().when(configManager.getRateLimitWindow("leak")).thenReturn(1L);
        lenient().when(configManager.getConfirmationTimeout("reload")).thenReturn(1L);

        protectionManager.applyCooldown(playerUUID, "leak");
        protectionManager.recordRateLimitUsage(playerUUID, "leak");
        protectionManager.initiateConfirmation(playerUUID, "reload");

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        protectionManager.cleanup();
    }
}
