package dev.cheat.client.command.impl;

import dev.cheat.client.Client;
import dev.cheat.client.command.Command;
import dev.cheat.client.util.ChatUtil;

public final class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Lists all commands.", "", "h", "?");
    }

    @Override
    public void execute(String[] args) {
        String prefix = Client.commands().getPrefix();
        ChatUtil.success(Client.NAME + " " + Client.VERSION + " commands:");
        for (Command command : Client.commands().getCommands()) {
            ChatUtil.info(prefix + command.getName() + " " + command.getUsage() + " - " + command.getDescription());
        }
    }
}
