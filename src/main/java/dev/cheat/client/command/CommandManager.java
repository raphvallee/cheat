package dev.cheat.client.command;

import dev.cheat.client.Client;
import dev.cheat.client.command.impl.BindCommand;
import dev.cheat.client.command.impl.ConfigCommand;
import dev.cheat.client.command.impl.HelpCommand;
import dev.cheat.client.command.impl.HideCommand;
import dev.cheat.client.command.impl.SetCommand;
import dev.cheat.client.command.impl.ToggleCommand;
import dev.cheat.client.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Parses chat input starting with {@link #getPrefix()} and runs the matching command. */
public final class CommandManager {
    private final List<Command> commands = new ArrayList<>();
    private String prefix = ".";

    /** Add new commands here. */
    public void registerDefaults() {
        register(
                new HelpCommand(),
                new ToggleCommand(),
                new BindCommand(),
                new SetCommand(),
                new HideCommand(),
                new ConfigCommand()
        );
    }

    public void register(Command... toAdd) {
        commands.addAll(Arrays.asList(toAdd));
    }

    public boolean isCommand(String message) {
        return message.startsWith(prefix) && message.length() > prefix.length();
    }

    public void execute(String message) {
        String[] parts = message.substring(prefix.length()).trim().split("\\s+");
        Command command = getCommand(parts[0]);
        if (command == null) {
            ChatUtil.error("Unknown command '" + parts[0] + "'. Type " + prefix + "help.");
            return;
        }
        try {
            command.execute(Arrays.copyOfRange(parts, 1, parts.length));
        } catch (CommandException e) {
            ChatUtil.error(e.getMessage());
            if (e.showUsage()) ChatUtil.info("Usage: " + prefix + command.getName() + " " + command.getUsage());
        } catch (RuntimeException e) {
            ChatUtil.error("Command failed: " + e);
            Client.LOGGER.error("Command {} failed", command.getName(), e);
        }
    }

    public Command getCommand(String name) {
        for (Command command : commands) {
            if (command.matches(name)) return command;
        }
        return null;
    }

    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
