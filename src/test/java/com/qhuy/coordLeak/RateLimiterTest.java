package com.qhuy.coordLeak;

import com.qhuy.coordLeak.managers.ConfigManager;
import com.qhuy.coordLeak.managers.ProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;

public class RateLimiterTest {
    @Test
    void testSlidingWindowLimit() {
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            Server server = Mockito.mock(Server.class);
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(() -> Bukkit.getPlayer(Mockito.any(UUID.class))).thenReturn(null);

            CoordLeak plugin = Mockito.mock(CoordLeak.class);
            ConfigManager cfg = Mockito.mock(ConfigManager.class);
            Mockito.when(cfg.getRateLimit("leak")).thenReturn(3);
            Mockito.when(cfg.getRateLimitWindow("leak")).thenReturn(1000L);
            ProtectionManager pm = new ProtectionManager(plugin, cfg);
            UUID u = UUID.randomUUID();
            for (int i = 0; i < 3; i++) {
                Assertions.assertFalse(pm.isRateLimited(u, "leak"));
                pm.recordRateLimitUsage(u, "leak");
            }
            Assertions.assertTrue(pm.isRateLimited(u, "leak"));
        }
    }
}
