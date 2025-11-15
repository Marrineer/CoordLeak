package com.qhuy.coordLeak.commands;

import com.qhuy.coordLeak.commands.subcommands.HelpCommand;
import com.qhuy.coordLeak.commands.subcommands.SubCommand;
import com.qhuy.coordLeak.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final MessageUtil messageUtil;
    private final SubCommand helpCommand;

    public CommandManager(MessageUtil messageUtil) {
        this.messageUtil = messageUtil;
        this.helpCommand = new HelpCommand(messageUtil);
        // Register the default help command so it can be triggered by "help"
        registerSubCommand(this.helpCommand);
    }

    public void registerSubCommand(@NotNull SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            helpCommand.execute(sender, new String[0]);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            helpCommand.execute(sender, new String[0]);
            return true;
        }

        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            messageUtil.send(sender, "no-permission");
            return true;
        }

        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return subCommands.values().stream()
                    .filter(sub -> sub.getName().startsWith(partial))
                    .filter(sub -> sub.getPermission() == null || sender.hasPermission(sub.getPermission()))
                    .map(SubCommand::getName)
                    .collect(Collectors.toList());
        }

        if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);
            if (subCommand != null) {
                if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                    String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
                    return subCommand.onTabComplete(sender, subCommandArgs);
                }
            }
        }

        return new ArrayList<>();
    }
}
