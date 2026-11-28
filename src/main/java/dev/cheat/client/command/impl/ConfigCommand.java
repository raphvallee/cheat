package dev.cheat.client.command.impl;

import dev.cheat.client.Client;
import dev.cheat.client.command.Command;
import dev.cheat.client.command.CommandException;
import dev.cheat.client.config.ConfigManager;
import dev.cheat.client.util.ChatUtil;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Saves, loads or lists named configs.", "<save|load|list> [name]", "cfg");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length == 0) throw CommandException.usage();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "save" -> {
                if (args.length != 2) throw CommandException.usage();
                if (!invalidName(() -> Client.config().save(args[1]))) {
                    throw new CommandException("Could not save config '" + args[1] + "', see the game log.");
                }
                ChatUtil.success("Saved config '" + args[1] + "'.");
            }
            case "load" -> {
                if (args.length != 2) throw CommandException.usage();
                ConfigManager.LoadResult result = invalidName(() -> Client.config().load(args[1]));
                switch (result) {
                    case NOT_FOUND -> throw new CommandException("Config '" + args[1] + "' not found.");
                    case INVALID -> throw new CommandException("Config '" + args[1] + "' is not valid JSON, see the game log.");
                    case LOADED -> ChatUtil.success("Loaded config '" + args[1] + "'.");
                }
            }
            case "list" -> {
                List<String> configs = Client.config().list();
                ChatUtil.info(configs.isEmpty() ? "No saved configs." : "Configs: " + String.join(", ", configs));
            }
            default -> throw CommandException.usage();
        }
    }

    private static <T> T invalidName(Supplier<T> action) throws CommandException {
        try {
            return action.get();
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage());
        }
    }
}
