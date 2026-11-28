package dev.cheat.client.command;

/**
 * Thrown by a command to report a user error.
 */
public final class CommandException extends Exception {
    private final boolean showUsage;

    public CommandException(String message) {
        this(message, false);
    }

    public CommandException(String message, boolean showUsage) {
        super(message);
        this.showUsage = showUsage;
    }

    /**
     * Shortcut for "wrong arguments": prints the command usage.
     */
    public static CommandException usage() {
        return new CommandException("Invalid arguments.", true);
    }

    public boolean showUsage() {
        return showUsage;
    }
}
