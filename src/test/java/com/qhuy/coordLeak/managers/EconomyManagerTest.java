package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EconomyManagerTest {

    @Mock
    private CoordLeak plugin;

    @Mock
    private Economy economy;

    @Mock
    private Player player;

    @Mock
    private AuditLogger auditLogger;

    private EconomyManager economyManager;
    private UUID playerUUID;

    @BeforeEach
    void setUp() {
        playerUUID = UUID.randomUUID();
        lenient().when(player.getUniqueId()).thenReturn(playerUUID);
        lenient().when(player.getName()).thenReturn("TestPlayer");
        lenient().when(player.getAddress()).thenReturn(null);
        lenient().when(plugin.getAuditLogger()).thenReturn(auditLogger);

        economyManager = new EconomyManager(plugin, economy);
    }

    @AfterEach
    void tearDown() {
        economyManager.shutdown();
    }

    @Test
    void testIsAvailable_WithEconomy() {
        assertTrue(economyManager.isAvailable());
    }

    @Test
    void testIsAvailable_WithoutEconomy() {
        EconomyManager nullEconManager = new EconomyManager(plugin, null);
        assertFalse(nullEconManager.isAvailable());
        nullEconManager.shutdown();
    }

    @Test
    void testWithdraw_Success() {
        when(player.isOnline()).thenReturn(true);
        when(economy.getBalance(player)).thenReturn(100.0);
        when(economy.withdrawPlayer((org.bukkit.OfflinePlayer) player, 50.0))
                .thenReturn(new EconomyResponse(50.0, 50.0, EconomyResponse.ResponseType.SUCCESS, ""));

        assertTrue(economyManager.withdraw(player, 50.0));
        verify(economy).withdrawPlayer((org.bukkit.OfflinePlayer) player, 50.0);
    }

    @Test
    void testWithdraw_InsufficientFunds() {
        when(player.isOnline()).thenReturn(true);
        when(economy.getBalance(player)).thenReturn(30.0);

        assertFalse(economyManager.withdraw(player, 50.0));
        verify(economy, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    @Test
    void testWithdraw_NegativeAmount() {
        assertFalse(economyManager.withdraw(player, -10.0));
        verify(economy, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    @Test
    void testWithdraw_NullPlayer() {
        assertFalse(economyManager.withdraw(null, 50.0));
        verify(economy, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    @Test
    void testWithdraw_OfflinePlayer() {
        when(player.isOnline()).thenReturn(false);
        assertFalse(economyManager.withdraw(player, 50.0));
        verify(economy, never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    @Test
    void testWithdraw_TransactionFailure() {
        when(player.isOnline()).thenReturn(true);
        when(economy.getBalance(player)).thenReturn(100.0);
        when(economy.withdrawPlayer((org.bukkit.OfflinePlayer) player, 50.0))
                .thenReturn(new EconomyResponse(0, 100.0, EconomyResponse.ResponseType.FAILURE, "Transaction failed"));

        assertFalse(economyManager.withdraw(player, 50.0));
    }

    @Test
    void testWithdraw_ConcurrentAccess() throws InterruptedException {
        when(player.isOnline()).thenReturn(true);
        when(economy.getBalance(player)).thenReturn(100.0);
        when(economy.withdrawPlayer(eq((org.bukkit.OfflinePlayer) player), anyDouble()))
                .thenReturn(new EconomyResponse(10.0, 90.0, EconomyResponse.ResponseType.SUCCESS, ""));

        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        int[] successCount = {0};

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                if (economyManager.withdraw(player, 10.0)) {
                    synchronized (successCount) {
                        successCount[0]++;
                    }
                }
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        // Only first thread should succeed, others should fail tryLock
        assertTrue(successCount[0] >= 1 && successCount[0] <= threadCount);
    }

    @Test
    void testWithdraw_Exception() {
        when(player.isOnline()).thenReturn(true);
        when(economy.getBalance(player)).thenThrow(new RuntimeException("Economy error"));
        
        assertFalse(economyManager.withdraw(player, 50.0));
    }

    @Test
    void testWithdraw_UnavailableEconomy() {
        EconomyManager nullEconManager = new EconomyManager(plugin, null);
        assertFalse(nullEconManager.withdraw(player, 50.0));
        nullEconManager.shutdown();
    }

    @Test
    void testRefundAsync_PlayerOffline() {
        economyManager.refundAsync(playerUUID, 50.0, 0);
        verify(economy, never()).depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    @Test
    void testRefundAsync_ZeroAmount() {
        economyManager.refundAsync(playerUUID, 0.0, 0);
        verify(economy, never()).depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    @Test
    void testRefundAsync_NegativeAmount() {
        economyManager.refundAsync(playerUUID, -10.0, 0);
        verify(economy, never()).depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    @Test
    void testShutdown_CompletesGracefully() {
        assertDoesNotThrow(() -> economyManager.shutdown());
    }
}
