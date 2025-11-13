package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Economy safety wrapper providing per-player locking and async refund.
 */
public class EconomyManager {
    private final CoordLeak plugin;
    private final Economy economy;
    private final Map<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService refundExecutor = Executors.newScheduledThreadPool(1);

    public EconomyManager(CoordLeak plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    private ReentrantLock lockFor(UUID uuid) {
        return locks.computeIfAbsent(uuid, k -> new ReentrantLock());
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean withdraw(Player player, double amount) {
        if (!isAvailable()) return false;
        if (amount < 0) return false;
        ReentrantLock lock = lockFor(player.getUniqueId());
        lock.lock();
        try {
            if (economy.getBalance(player) < amount) return false;
            EconomyResponse resp = economy.withdrawPlayer(player, amount);
            if (!resp.transactionSuccess()) {
                plugin.getLogger().severe("Withdraw failed for " + player.getName() + ": " + resp.errorMessage);
                return false;
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void refundAsync(UUID uuid, double amount, int attempt) {
        if (!isAvailable() || amount <= 0) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return; // offline refund skipped (could be queued if needed)
        refundExecutor.execute(() -> {
            ReentrantLock lock = lockFor(uuid);
            lock.lock();
            try {
                EconomyResponse resp = economy.depositPlayer(player, amount);
                if (!resp.transactionSuccess()) {
                    if (attempt < 3) {
                        // retry with backoff
                        refundExecutor.schedule(() -> refundAsync(uuid, amount, attempt + 1), 1L << attempt, TimeUnit.SECONDS);
                    } else {
                        plugin.getLogger().warning("Refund failed after retries for " + player.getName() + ": " + resp.errorMessage);
                        plugin.getAuditLogger().log("REFUND_FAIL", player.getName(), "N/A", amount, "FAILED", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Refund failed after retries: " + resp.errorMessage);
                    }
                } else {
                    plugin.getAuditLogger().log("REFUND", player.getName(), "N/A", amount, "SUCCESS", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A", "Refunded amount " + amount);
                }
            } finally {
                lock.unlock();
            }
        });
    }

    public void shutdown() {
        refundExecutor.shutdown();
    }
}
