package com.qhuy.coordLeak.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageUtilTest {

    @Test
    void testFormatTime_Seconds() {
        assertEquals("5s", MessageUtil.formatTime(5000));
        assertEquals("30s", MessageUtil.formatTime(30000));
        assertEquals("59s", MessageUtil.formatTime(59000));
    }

    @Test
    void testFormatTime_Minutes() {
        assertEquals("1m 0s", MessageUtil.formatTime(60000));
        assertEquals("2m 30s", MessageUtil.formatTime(150000));
        assertEquals("59m 59s", MessageUtil.formatTime(3599000));
    }

    @Test
    void testFormatTime_Hours() {
        assertEquals("1h 0m 0s", MessageUtil.formatTime(3600000));
        assertEquals("2h 30m 45s", MessageUtil.formatTime(9045000));
        assertEquals("24h 0m 0s", MessageUtil.formatTime(86400000));
    }

    @Test
    void testFormatTime_Zero() {
        assertEquals("0s", MessageUtil.formatTime(0));
    }

    @Test
    void testFormatTime_LessThanSecond() {
        assertEquals("0s", MessageUtil.formatTime(500));
    }

    @Test
    void testFormatTime_EdgeCases() {
        assertEquals("1s", MessageUtil.formatTime(1000));
        assertEquals("1m 0s", MessageUtil.formatTime(60000));
        assertEquals("1h 1m 1s", MessageUtil.formatTime(3661000));
    }
}
