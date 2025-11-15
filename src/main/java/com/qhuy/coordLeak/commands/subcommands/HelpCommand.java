package com.qhuy.coordLeak.commands.subcommands;

import com.qhuy.coordLeak.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class HelpCommand implements SubCommand {
    private final MessageUtil messageUtil;

    public HelpCommand(MessageUtil messageUtil) {
        this.messageUtil = messageUtil;
    }

    @Override
    public @NotNull String getName() {
        return "help";
    }

    @Override
    public @Nullable String getPermission() {
        return null; // No permission required to see help
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // The original command used "info.layout" for the help message.
        messageUtil.sendList(sender, "info.layout");
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
