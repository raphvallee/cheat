package dev.cheat.client.command.impl;

import dev.cheat.client.command.Command;
import dev.cheat.client.command.CommandException;
import dev.cheat.client.module.Module;
import dev.cheat.client.util.ChatUtil;

public final class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "Turns a module on or off.", "<module>", "t");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length != 1) throw CommandException.usage();
        Module module = Modules.find(args[0]);
        module.toggle();
        ChatUtil.info(module.getName() + " " + (module.isEnabled() ? "enabled" : "disabled") + ".");
    }
}
