package com.qhuy.coordLeak;

import com.qhuy.coordLeak.managers.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class EconomyManagerTest {
    @Test
    void testConcurrentWithdraw() throws InterruptedException {
        CoordLeak plugin = Mockito.mock(CoordLeak.class);
        Economy economy = Mockito.mock(Economy.class);
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        Mockito.when(economy.getBalance(player)).thenReturn(100.0);
        Mockito.when(economy.withdrawPlayer(Mockito.eq(player), Mockito.anyDouble()))
                .thenAnswer(inv -> new EconomyResponse(inv.getArgument(1), 0, EconomyResponse.ResponseType.SUCCESS, ""));
        EconomyManager em = new EconomyManager(plugin, economy);
        Runnable task = () -> Assertions.assertTrue(em.withdraw(player, 10.0));
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
