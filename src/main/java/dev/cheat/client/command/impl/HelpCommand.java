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
            StringBuilder line = new StringBuilder(prefix).append(command.getName());
            if (!command.getUsage().isEmpty()) line.append(' ').append(command.getUsage());
            line.append(" - ").append(command.getDescription());
            if (!command.getAliases().isEmpty()) {
                line.append(" (aliases: ").append(String.join(", ", command.getAliases())).append(')');
            }
            ChatUtil.info(line.toString());
        }
    }
}
