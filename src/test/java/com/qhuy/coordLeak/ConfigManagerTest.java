package com.qhuy.coordLeak;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigManagerTest {
    @Test
    void testSecondsToMillis() {
        long seconds = 5L;
        long expected = 5000L;
        long actual = seconds * 1000L;
        Assertions.assertEquals(expected, actual);
    }
}
