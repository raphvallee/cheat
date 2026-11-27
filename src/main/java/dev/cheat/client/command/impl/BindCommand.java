package dev.cheat.client.command.impl;

import dev.cheat.client.command.Command;
import dev.cheat.client.command.CommandException;
import dev.cheat.client.module.Module;
import dev.cheat.client.util.ChatUtil;
import dev.cheat.client.util.KeyUtil;

public final class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Binds a module to a key (or 'none').", "<module> <key>", "b");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length != 2) throw CommandException.usage();
        Module module = Modules.find(args[0]);
        int key = KeyUtil.parse(args[1]);
        if (key == KeyUtil.NONE && !args[1].equalsIgnoreCase("none")) {
            throw new CommandException("Unknown key '" + args[1] + "'.");
        }
        module.setKey(key);
        ChatUtil.info(module.getName() + " bound to " + KeyUtil.name(key) + ".");
    }
}
