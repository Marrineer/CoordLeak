package com.qhuy.coordLeak.utils;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerUtilTest {

    @Mock
    private Player player;

    @Mock
    private InetSocketAddress address;

    @Mock
    private InetAddress inetAddress;

    @Test
    void testGetPlayerIp_ValidPlayer() {
        when(player.getAddress()).thenReturn(address);
        when(address.getAddress()).thenReturn(inetAddress);
        when(inetAddress.getHostAddress()).thenReturn("192.168.1.1");

        assertEquals("192.168.1.1", PlayerUtil.getPlayerIp(player));
    }

    @Test
    void testGetPlayerIp_NullPlayer() {
        assertEquals("unknown", PlayerUtil.getPlayerIp(null));
    }

    @Test
    void testGetPlayerIp_NullAddress() {
        when(player.getAddress()).thenReturn(null);
        assertEquals("unknown", PlayerUtil.getPlayerIp(player));
    }

    @Test
    void testGetPlayerIp_NullInetAddress() {
        when(player.getAddress()).thenReturn(address);
        when(address.getAddress()).thenReturn(null);
        assertEquals("unknown", PlayerUtil.getPlayerIp(player));
    }

    @Test
    void testGetLoggableIp_Enabled() {
        when(player.getAddress()).thenReturn(address);
        when(address.getAddress()).thenReturn(inetAddress);
        when(inetAddress.getHostAddress()).thenReturn("192.168.1.1");

        assertEquals("192.168.1.1", PlayerUtil.getLoggableIp(player, true));
    }

    @Test
    void testGetLoggableIp_Disabled() {
        assertEquals("REDACTED", PlayerUtil.getLoggableIp(player, false));
    }

    @Test
    void testGetSafeName_ValidPlayer() {
        when(player.getName()).thenReturn("TestPlayer");
        assertEquals("TestPlayer", PlayerUtil.getSafeName(player));
    }

    @Test
    void testGetSafeName_NullPlayer() {
        assertEquals("Console", PlayerUtil.getSafeName(null));
    }

    @Test
    void testIsPlayerValid_ValidPlayer() {
        when(player.isOnline()).thenReturn(true);
        when(player.getWorld()).thenReturn(mock(org.bukkit.World.class));
        when(player.getLocation()).thenReturn(mock(org.bukkit.Location.class));

        assertTrue(PlayerUtil.isPlayerValid(player));
    }

    @Test
    void testIsPlayerValid_NullPlayer() {
        assertFalse(PlayerUtil.isPlayerValid(null));
    }

    @Test
    void testIsPlayerValid_OfflinePlayer() {
        when(player.isOnline()).thenReturn(false);
        assertFalse(PlayerUtil.isPlayerValid(player));
    }

    @Test
    void testIsPlayerValid_NullWorld() {
        when(player.isOnline()).thenReturn(true);
        when(player.getWorld()).thenReturn(null);
        assertFalse(PlayerUtil.isPlayerValid(player));
    }

    @Test
    void testIsPlayerValid_NullLocation() {
        when(player.isOnline()).thenReturn(true);
        when(player.getWorld()).thenReturn(mock(org.bukkit.World.class));
        when(player.getLocation()).thenReturn(null);
        assertFalse(PlayerUtil.isPlayerValid(player));
    }

    @Test
    void testIsPlayerValid_ExceptionThrown() {
        when(player.isOnline()).thenReturn(true);
        when(player.getWorld()).thenThrow(new RuntimeException("World unloaded"));
        assertFalse(PlayerUtil.isPlayerValid(player));
    }
}
