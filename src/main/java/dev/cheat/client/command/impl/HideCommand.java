package dev.cheat.client.command.impl;

import dev.cheat.client.command.Command;
import dev.cheat.client.command.CommandException;
import dev.cheat.client.module.Module;
import dev.cheat.client.util.ChatUtil;

public final class HideCommand extends Command {
    public HideCommand() {
        super("hide", "Hides or shows a module in the HUD module list.", "<module>");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length != 1) throw CommandException.usage();
        Module module = Modules.find(args[0]);
        module.setHidden(!module.isHidden());
        ChatUtil.info(module.getName() + " is now " + (module.isHidden() ? "hidden" : "shown") + ".");
    }
}
