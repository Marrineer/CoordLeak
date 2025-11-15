package com.qhuy.coordLeak.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a subcommand for the main plugin command.
 */
public interface SubCommand {

    /**
     * Gets the name of the subcommand. This is the string that users will type.
     *
     * @return The name of the subcommand.
     */
    @NotNull
    String getName();

    /**
     * Gets the permission required to execute this subcommand.
     *
     * @return The permission string, or null if no permission is required.
     */
    @Nullable
    String getPermission();

    /**
     * Executes the subcommand logic.
     *
     * @param sender The CommandSender who executed the command.
     * @param args   The arguments provided with the command, excluding the subcommand name itself.
     */
    void execute(@NotNull CommandSender sender, @NotNull String[] args);

    /**
     * Provides tab completions for this subcommand.
     *
     * @param sender The CommandSender requesting completions.
     * @param args   The arguments provided so far.
     * @return A list of suggested completions.
     */
    @NotNull
    List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args);
}
