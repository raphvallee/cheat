package dev.cheat.client.command.impl;

import dev.cheat.client.Client;
import dev.cheat.client.command.Command;
import dev.cheat.client.command.CommandException;
import dev.cheat.client.util.ChatUtil;

import java.util.List;

public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Saves, loads or lists named configs.", "<save|load|list> [name]", "cfg");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length == 0) throw CommandException.usage();
        switch (args[0].toLowerCase()) {
            case "save" -> {
                if (args.length != 2) throw CommandException.usage();
                Client.config().save(args[1]);
                ChatUtil.success("Saved config '" + args[1] + "'.");
            }
            case "load" -> {
                if (args.length != 2) throw CommandException.usage();
                if (!Client.config().load(args[1])) throw new CommandException("Config '" + args[1] + "' not found.");
                ChatUtil.success("Loaded config '" + args[1] + "'.");
            }
            case "list" -> {
                List<String> configs = Client.config().list();
                ChatUtil.info(configs.isEmpty() ? "No saved configs." : "Configs: " + String.join(", ", configs));
            }
            default -> throw CommandException.usage();
        }
    }
}
