package com.qhuy.coordLeak;

import com.qhuy.coordLeak.managers.MessageManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.logging.Logger;

public class MessageManagerTest {
    @Test
    void testInstantiate() {
        CoordLeak plugin = Mockito.mock(CoordLeak.class);
        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(plugin.getLogger()).thenReturn(logger);
        Mockito.when(plugin.getDataFolder()).thenReturn(new File(System.getProperty("java.io.tmpdir")));
        Mockito.when(plugin.getResource("messages.yml")).thenReturn(null);

        Assertions.assertDoesNotThrow(() -> new MessageManager(plugin));
    }
}
