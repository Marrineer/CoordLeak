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
 * Production-grade economy wrapper providing thread-safety and graceful error handling.
 */
public final class EconomyManager {
    private final CoordLeak plugin;
    private final Economy economy;
    private final Map<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService refundExecutor;
    private volatile boolean shuttingDown = false;

    public EconomyManager(CoordLeak plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.refundExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "CoordLeak-Refund");
            thread.setDaemon(true);
            return thread;
        });
    }

    private ReentrantLock lockFor(UUID uuid) {
        return locks.computeIfAbsent(uuid, k -> new ReentrantLock());
    }

    public boolean isAvailable() {
        return economy != null && !shuttingDown;
    }

    /**
     * Safely withdraws money from player with proper locking.
     * @param player Player to withdraw from
     * @param amount Amount to withdraw
     * @return true if successful
     */
    public boolean withdraw(Player player, double amount) {
        if (!isAvailable()) {
            plugin.getLogger().warning("Economy withdraw attempted but service unavailable");
            return false;
        }
        
        if (amount < 0) {
            plugin.getLogger().warning("Attempted negative withdraw: " + amount);
            return false;
        }
        
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Attempted withdraw on null/offline player");
            return false;
        }

        ReentrantLock lock = lockFor(player.getUniqueId());
        if (!lock.tryLock()) {
            plugin.getLogger().warning("Concurrent withdraw detected for " + player.getName());
            return false;
        }
        
        try {
            double balance = economy.getBalance(player);
            if (balance < amount) {
                return false;
            }
            
            EconomyResponse resp = economy.withdrawPlayer(player, amount);
            if (!resp.transactionSuccess()) {
                plugin.getLogger().severe("Withdraw failed for " + player.getName() + ": " + resp.errorMessage);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Exception during withdraw for " + player.getName() + ": " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Async refund with retry logic.
     * @param uuid Player UUID
     * @param amount Amount to refund
     * @param attempt Current attempt number (0-based)
     */
    public void refundAsync(UUID uuid, double amount, int attempt) {
        if (!isAvailable() || amount <= 0 || shuttingDown) {
            return;
        }

        refundExecutor.execute(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                if (attempt == 0) {
                    plugin.getLogger().info("Skipping refund for offline player: " + uuid);
                }
                return;
            }

            ReentrantLock lock = lockFor(uuid);
            lock.lock();
            try {
                EconomyResponse resp = economy.depositPlayer(player, amount);
                if (!resp.transactionSuccess()) {
                    if (attempt < 3) {
                        long delay = 1L << attempt; // Exponential backoff: 1s, 2s, 4s
                        refundExecutor.schedule(() -> refundAsync(uuid, amount, attempt + 1), delay, TimeUnit.SECONDS);
                    } else {
                        plugin.getLogger().warning("Refund failed after retries for " + player.getName() + ": " + resp.errorMessage);
                        plugin.getAuditLogger().log("REFUND_FAIL", player.getName(), "N/A", amount, "FAILED", "N/A", "Refund failed after retries: " + resp.errorMessage);
                    }
                } else {
                    plugin.getAuditLogger().log("REFUND", player.getName(), "N/A", amount, "SUCCESS", "N/A", "Refunded amount " + amount);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Exception during refund for " + player.getName() + ": " + e.getMessage());
            } finally {
                lock.unlock();
            }
        });
    }

    /**
     * Gracefully shuts down the refund executor.
     * Must be called during plugin disable.
     */
    public void shutdown() {
        shuttingDown = true;
        refundExecutor.shutdown();
        try {
            if (!refundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Refund executor did not terminate cleanly, forcing shutdown");
                refundExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            plugin.getLogger().warning("Interrupted during refund executor shutdown");
            refundExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        locks.clear();
    }
}
