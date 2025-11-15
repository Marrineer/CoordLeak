package com.qhuy.coordLeak.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SanitizerTest {

    private Sanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new Sanitizer();
    }

    @Test
    void testSanitize_NormalText() {
        String input = "Hello World";
        String result = sanitizer.sanitize(input);
        assertEquals("Hello World", result);
    }

    @Test
    void testSanitize_RemovesMiniMessageTags() {
        String input = "<gradient:#FF0000:#00FF00>Malicious Text</gradient>";
        String result = sanitizer.sanitize(input);
        assertEquals("Malicious Text", result);
    }

    @Test
    void testSanitize_RemovesColorTags() {
        String input = "<red>Red Text</red>";
        String result = sanitizer.sanitize(input);
        assertEquals("Red Text", result);
    }

    @Test
    void testSanitize_PreservesPlainText() {
        String input = "Just plain text with numbers 123 and symbols !@#$%";
        String result = sanitizer.sanitize(input);
        assertTrue(result.contains("plain text"));
        assertTrue(result.contains("123"));
    }

    @Test
    void testSanitize_EmptyString() {
        String input = "";
        String result = sanitizer.sanitize(input);
        assertEquals("", result);
    }

    @Test
    void testSanitize_NullInput() {
        assertDoesNotThrow(() -> sanitizer.sanitize(null));
    }

    @Test
    void testSanitize_ComplexMiniMessage() {
        String input = "<hover:show_text:'Click me'><click:run_command:/evil>Malicious</click></hover>";
        String result = sanitizer.sanitize(input);
        assertEquals("Malicious", result);
    }

    @Test
    void testSanitize_MultipleTagsInSequence() {
        String input = "<bold><italic><underlined>Text</underlined></italic></bold>";
        String result = sanitizer.sanitize(input);
        assertEquals("Text", result);
    }

    @Test
    void testSanitize_EscapesPlaceholderAPI() {
        String input = "Hello %player_name%";
        String result = sanitizer.sanitize(input);
        assertEquals("Hello %%player_name%%", result);
    }

    @Test
    void testSanitize_CombinedTagsAndPlaceholders() {
        String input = "<red>Hello %player%</red>";
        String result = sanitizer.sanitize(input);
        assertEquals("Hello %%player%%", result);
    }
}
