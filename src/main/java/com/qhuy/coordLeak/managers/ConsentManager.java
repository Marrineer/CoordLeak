package com.qhuy.coordLeak.managers;

import com.qhuy.coordLeak.CoordLeak;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages consent requests for coordinate sharing.
 */
public class ConsentManager {
    private final CoordLeak plugin;
    private final Map<String, ConsentRequest> pendingRequests = new ConcurrentHashMap<>();
    private final long REQUEST_TIMEOUT = 30000L; // 30s
    public ConsentManager(CoordLeak plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, 20L * 60, 20L * 60); // cleanup every 60s
    }

    public String createRequest(UUID requester, UUID target) {
        String requestId = generateRequestId();
        ConsentRequest request = new ConsentRequest(requester, target, requestId);
        pendingRequests.put(requestId, request);

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            String senderName = "A player";
            Player requesterPlayer = Bukkit.getPlayer(requester);
            if (requesterPlayer != null) {
                senderName = requesterPlayer.getName();
            }
            plugin.getMessageUtil().send(targetPlayer, "consent.request",
                    "%sender%", senderName,
                    "%id%", requestId);
        }

        return requestId;
    }

    public boolean acceptRequest(String requestId, UUID acceptor) {
        ConsentRequest req = pendingRequests.get(requestId);
        if (req == null) return false;
        if (!req.target.equals(acceptor)) return false;
        if (System.currentTimeMillis() - req.timestamp > REQUEST_TIMEOUT) {
            pendingRequests.remove(requestId);
            return false;
        }
        pendingRequests.remove(requestId);
        return true;
    }

    public boolean denyRequest(String requestId, UUID denier) {
        ConsentRequest req = pendingRequests.get(requestId);
        if (req == null) return false;
        if (!req.target.equals(denier)) return false;
        pendingRequests.remove(requestId);

        Player requester = Bukkit.getPlayer(req.requester);
        if (requester != null) {
            plugin.getMessageUtil().send(requester, "consent.denied");
        }
        return true;
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(entry -> (now - entry.getValue().timestamp) > REQUEST_TIMEOUT);
    }

    private static class ConsentRequest {
        final UUID requester;
        final UUID target;
        final long timestamp;
        final String requestId;

        ConsentRequest(UUID requester, UUID target, String requestId) {
            this.requester = requester;
            this.target = target;
            this.requestId = requestId;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
