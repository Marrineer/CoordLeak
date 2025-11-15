package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigManagerTest {

    @Mock
    private CoordLeak plugin;

    @Mock
    private FileConfiguration config;

    @Mock
    private java.util.logging.Logger logger;

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        lenient().when(plugin.getConfig()).thenReturn(config);
        lenient().when(plugin.getLogger()).thenReturn(logger);
        setupDefaultConfig();
    }

    private void setupDefaultConfig() {
        lenient().when(config.getString(eq("prefix"), anyString())).thenReturn("<i><gradient:#FFFFFF:#29E7D7>[ Coord ]</gradient></i>");
        lenient().when(config.getDouble(eq("price.default"), anyDouble())).thenReturn(50.0);
        lenient().when(config.getDouble(eq("price.min"), anyDouble())).thenReturn(1.0);
        lenient().when(config.getDouble(eq("price.max"), anyDouble())).thenReturn(100000.0);
        lenient().when(config.getLong(eq("cooldowns.leak"), anyLong())).thenReturn(5L);
        lenient().when(config.getLong(eq("cooldowns.share"), anyLong())).thenReturn(2L);
        lenient().when(config.getLong(eq("cooldowns.setprice"), anyLong())).thenReturn(5L);
        lenient().when(config.getInt(eq("ratelimit.leak.limit"), anyInt())).thenReturn(5);
        lenient().when(config.getLong(eq("ratelimit.leak.window"), anyLong())).thenReturn(TimeUnit.SECONDS.toMillis(60));
        lenient().when(config.getInt(eq("ratelimit.share.limit"), anyInt())).thenReturn(10);
        lenient().when(config.getLong(eq("ratelimit.share.window"), anyLong())).thenReturn(TimeUnit.SECONDS.toMillis(30));
        lenient().when(config.getInt(eq("ratelimit.setprice.limit"), anyInt())).thenReturn(1);
        lenient().when(config.getLong(eq("ratelimit.setprice.window"), anyLong())).thenReturn(TimeUnit.SECONDS.toMillis(10));
        lenient().when(config.getBoolean(eq("global.ratelimit.enabled"), anyBoolean())).thenReturn(true);
        lenient().when(config.getInt(eq("global.ratelimit.limit"), anyInt())).thenReturn(200);
        lenient().when(config.getLong(eq("global.ratelimit.window"), anyLong())).thenReturn(TimeUnit.MINUTES.toMillis(1));
        lenient().when(config.getLong(eq("global.ratelimit.block-duration"), anyLong())).thenReturn(TimeUnit.SECONDS.toMillis(10));
        lenient().when(config.getInt(eq("limits.daily.leak"), anyInt())).thenReturn(50);
        lenient().when(config.getInt(eq("limits.daily.share"), anyInt())).thenReturn(100);
        lenient().when(config.getBoolean(eq("audit.enabled"), anyBoolean())).thenReturn(true);
        lenient().when(config.getString(eq("audit.log-file"), anyString())).thenReturn("logs/coordleak.log");
        lenient().when(config.isSet(eq("audit.log-ip-address"))).thenReturn(true);
        lenient().when(config.getBoolean(eq("audit.log-ip-address"))).thenReturn(false);
        lenient().when(config.getBoolean(eq("reload.require-confirm"), anyBoolean())).thenReturn(true);
        lenient().when(config.getLong(eq("reload.confirmation-timeout"), anyLong())).thenReturn(TimeUnit.SECONDS.toMillis(10));
        lenient().when(config.getBoolean(eq("blacklist.enabled"), anyBoolean())).thenReturn(false);
        lenient().when(config.getStringList(eq("blacklist.uuids"))).thenReturn(Arrays.asList());
        lenient().when(config.getBoolean(eq("whitelist.enabled"), anyBoolean())).thenReturn(false);
        lenient().when(config.getStringList(eq("whitelist.uuids"))).thenReturn(Arrays.asList());
        lenient().when(config.getStringList(eq("target.exclude-worlds"))).thenReturn(Arrays.asList());
        lenient().when(config.getStringList(eq("target.exclude-permissions"))).thenReturn(Arrays.asList());
        lenient().when(config.getBoolean(eq("target.require-consent-for-share"), anyBoolean())).thenReturn(false);
    }

    @Test
    void testLoadConfig_DefaultValues() {
        configManager = new ConfigManager(plugin);

        assertEquals(50.0, configManager.getDefaultPrice());
        assertEquals(1.0, configManager.getMinPrice());
        assertEquals(100000.0, configManager.getMaxPrice());
    }

    @Test
    void testGetCooldownMillis_LeakCommand() {
        configManager = new ConfigManager(plugin);
        assertEquals(5000L, configManager.getCooldownMillis("leak"));
    }

    @Test
    void testGetCooldownMillis_ShareCommand() {
        configManager = new ConfigManager(plugin);
        assertEquals(2000L, configManager.getCooldownMillis("share"));
    }

    @Test
    void testGetCooldownMillis_SetPriceCommand() {
        configManager = new ConfigManager(plugin);
        assertEquals(5000L, configManager.getCooldownMillis("setprice"));
    }

    @Test
    void testGetCooldownMillis_UnknownCommand() {
        configManager = new ConfigManager(plugin);
        assertEquals(0L, configManager.getCooldownMillis("unknown"));
    }

    @Test
    void testGetRateLimit_ValidCommands() {
        configManager = new ConfigManager(plugin);
        assertEquals(5, configManager.getRateLimit("leak"));
        assertEquals(10, configManager.getRateLimit("share"));
        assertEquals(1, configManager.getRateLimit("setprice"));
    }

    @Test
    void testGetRateLimitWindow_ValidCommands() {
        configManager = new ConfigManager(plugin);
        assertEquals(60000L, configManager.getRateLimitWindow("leak"));
        assertEquals(30000L, configManager.getRateLimitWindow("share"));
        assertEquals(10000L, configManager.getRateLimitWindow("setprice"));
    }

    @Test
    void testGlobalRateLimitSettings() {
        configManager = new ConfigManager(plugin);
        assertTrue(configManager.isGlobalRateLimitEnabled());
        assertEquals(200, configManager.getGlobalRateLimit());
        assertEquals(60000L, configManager.getGlobalRateLimitWindow());
        assertEquals(10000L, configManager.getGlobalRateLimitBlockDuration());
    }

    @Test
    void testDailyLimits() {
        configManager = new ConfigManager(plugin);
        assertEquals(50, configManager.getDailyLimit("leak"));
        assertEquals(100, configManager.getDailyLimit("share"));
        assertEquals(0, configManager.getDailyLimit("unknown"));
    }

    @Test
    void testAuditSettings() {
        configManager = new ConfigManager(plugin);
        assertTrue(configManager.isAuditLoggingEnabled());
        assertEquals("logs/coordleak.log", configManager.getAuditLogFile());
        assertFalse(configManager.isIpLoggingEnabled());
    }

    @Test
    void testIpLogging_DeprecatedFallback() {
        when(config.isSet(eq("audit.log-ip-address"))).thenReturn(false);
        when(config.isSet(eq("audit.log-sensitive"))).thenReturn(true);
        when(config.getBoolean(eq("audit.log-sensitive"))).thenReturn(true);

        configManager = new ConfigManager(plugin);
        assertTrue(configManager.isIpLoggingEnabled());
        verify(plugin.getLogger()).warning(contains("deprecated"));
    }

    @Test
    void testBlacklistWhitelist() {
        List<String> blacklistedUUIDs = Arrays.asList("uuid1", "uuid2");
        List<String> whitelistedUUIDs = Arrays.asList("uuid3", "uuid4");
        when(config.getBoolean(eq("blacklist.enabled"), anyBoolean())).thenReturn(true);
        when(config.getStringList(eq("blacklist.uuids"))).thenReturn(blacklistedUUIDs);
        when(config.getBoolean(eq("whitelist.enabled"), anyBoolean())).thenReturn(true);
        when(config.getStringList(eq("whitelist.uuids"))).thenReturn(whitelistedUUIDs);

        configManager = new ConfigManager(plugin);
        assertTrue(configManager.isBlacklistEnabled());
        assertEquals(2, configManager.getBlacklistedUUIDs().size());
        assertTrue(configManager.isWhitelistEnabled());
        assertEquals(2, configManager.getWhitelistedUUIDs().size());
    }

    @Test
    void testTargetExclusions() {
        List<String> excludedWorlds = Arrays.asList("nether", "end");
        List<String> excludedPermissions = Arrays.asList("coordleak.exclude", "admin.bypass");
        when(config.getStringList(eq("target.exclude-worlds"))).thenReturn(excludedWorlds);
        when(config.getStringList(eq("target.exclude-permissions"))).thenReturn(excludedPermissions);

        configManager = new ConfigManager(plugin);
        assertEquals(2, configManager.getExcludedWorlds().size());
        assertEquals(2, configManager.getExcludedPermissions().size());
        assertTrue(configManager.getExcludedWorlds().contains("nether"));
    }

    @Test
    void testSetDefaultPrice() {
        configManager = new ConfigManager(plugin);
        configManager.setDefaultPrice(100.0);
        verify(config).set(eq("price.default"), eq(100.0));
        verify(plugin).saveConfig();
    }

    @Test
    void testConsentRequired() {
        when(config.getBoolean(eq("target.require-consent-for-share"), anyBoolean())).thenReturn(true);
        configManager = new ConfigManager(plugin);
        assertTrue(configManager.isConsentRequiredForShare());
    }

    @Test
    void testReloadRequireConfirm() {
        configManager = new ConfigManager(plugin);
        assertTrue(configManager.isReloadRequireConfirm());
        assertEquals(10000L, configManager.getConfirmationTimeout("reload"));
    }

    @Test
    void testGetPrefix() {
        configManager = new ConfigManager(plugin);
        String prefix = configManager.getPrefix();
        assertNotNull(prefix);
        assertTrue(prefix.contains("Coord"));
    }

    @Test
    void testDeprecatedGetCooldown() {
        configManager = new ConfigManager(plugin);
        assertEquals(5000L, configManager.getCooldown("leak"));
    }
}
