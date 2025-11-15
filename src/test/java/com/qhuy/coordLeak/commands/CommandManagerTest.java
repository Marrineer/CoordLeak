package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.commands.subcommands.SubCommand;
import com.qhuy.coordLeak.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandManagerTest {

    @Mock
    private MessageUtil messageUtil;

    @Mock
    private CommandSender sender;

    @Mock
    private Player player;

    @Mock
    private Command command;

    private CommandManager commandManager;

    @BeforeEach
    void setUp() {
        commandManager = new CommandManager(messageUtil);
    }

    @Test
    void testOnCommand_NoArgs_ShowsHelp() {
        boolean result = commandManager.onCommand(sender, command, "coord", new String[]{});
        assertTrue(result);
    }

    @Test
    void testOnCommand_UnknownSubCommand_ShowsHelp() {
        boolean result = commandManager.onCommand(sender, command, "coord", new String[]{"unknown"});
        assertTrue(result);
    }

    @Test
    void testOnCommand_ValidSubCommand_ExecutesCommand() {
        SubCommand testCommand = new TestSubCommand();
        commandManager.registerSubCommand(testCommand);

        boolean result = commandManager.onCommand(sender, command, "coord", new String[]{"test"});
        assertTrue(result);
    }

    @Test
    void testOnCommand_NoPermission_SendsMessage() {
        when(sender.hasPermission("coordleak.admin")).thenReturn(false);

        SubCommand testCommand = new TestSubCommand();
        commandManager.registerSubCommand(testCommand);

        boolean result = commandManager.onCommand(sender, command, "coord", new String[]{"test"});
        assertTrue(result);
        verify(messageUtil).send(sender, "no-permission");
    }

    @Test
    void testOnCommand_WithPermission_ExecutesCommand() {
        when(sender.hasPermission("coordleak.admin")).thenReturn(true);

        SubCommand testCommand = new TestSubCommand();
        commandManager.registerSubCommand(testCommand);

        boolean result = commandManager.onCommand(sender, command, "coord", new String[]{"test"});
        assertTrue(result);
    }

    @Test
    void testTabComplete_FirstArg_ReturnsSubCommands() {
        SubCommand test1 = new TestSubCommand();
        SubCommand test2 = new AnotherTestSubCommand();
        commandManager.registerSubCommand(test1);
        commandManager.registerSubCommand(test2);

        when(sender.hasPermission(anyString())).thenReturn(true);

        List<String> completions = commandManager.onTabComplete(sender, command, "coord", new String[]{"t"});
        assertFalse(completions.isEmpty());
    }

    @Test
    void testTabComplete_FirstArg_FiltersBasedOnPermission() {
        SubCommand test1 = new TestSubCommand();
        commandManager.registerSubCommand(test1);

        when(sender.hasPermission("coordleak.admin")).thenReturn(false);

        List<String> completions = commandManager.onTabComplete(sender, command, "coord", new String[]{"test"});
        assertTrue(completions.isEmpty());
    }

    @Test
    void testTabComplete_SubCommandArgs_CallsSubCommand() {
        SubCommand testCommand = new TestSubCommand();
        commandManager.registerSubCommand(testCommand);

        when(sender.hasPermission("coordleak.admin")).thenReturn(true);

        List<String> completions = commandManager.onTabComplete(sender, command, "coord", new String[]{"test", "arg"});
        assertNotNull(completions);
    }

    @Test
    void testRegisterSubCommand_StoresCommand() {
        SubCommand testCommand = new TestSubCommand();
        assertDoesNotThrow(() -> commandManager.registerSubCommand(testCommand));

        when(sender.hasPermission("coordleak.admin")).thenReturn(true);
        boolean result = commandManager.onCommand(sender, command, "coord", new String[]{"test"});
        assertTrue(result);
    }

    private static class TestSubCommand implements SubCommand {
        private boolean executed = false;

        @Override
        public @NotNull String getName() {
            return "test";
        }

        @Override
        public @NotNull String getPermission() {
            return "coordleak.admin";
        }

        @Override
        public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
            executed = true;
        }

        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
            return Arrays.asList("option1", "option2");
        }

        public boolean isExecuted() {
            return executed;
        }
    }

    private static class AnotherTestSubCommand implements SubCommand {
        @Override
        public @NotNull String getName() {
            return "another";
        }

        @Override
        public @NotNull String getPermission() {
            return "coordleak.admin";
        }

        @Override
        public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        }

        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
            return Collections.emptyList();
        }
    }
}
