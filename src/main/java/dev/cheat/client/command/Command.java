package dev.cheat.client.command;

import dev.cheat.client.util.MinecraftInstance;

import java.util.List;

/**
 * A chat command, run by typing the prefix (default {@code .}) and its name, e.g. {@code .toggle Sprint}.
 * Register new commands in {@link CommandManager#registerDefaults()}.
 */
public abstract class Command implements MinecraftInstance {
    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;

    protected Command(String name, String description, String usage, String... aliases) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.aliases = List.of(aliases);
    }

    /**
     * @param args arguments after the command name, split on whitespace
     * @throws CommandException to print an error (and the usage when {@code showUsage} is set)
     */
    public abstract void execute(String[] args) throws CommandException;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** Arguments syntax, e.g. {@code <module> <key>}. */
    public String getUsage() {
        return usage;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public boolean matches(String input) {
        if (name.equalsIgnoreCase(input)) return true;
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }
}
