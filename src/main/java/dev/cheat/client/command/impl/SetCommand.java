package dev.cheat.client.command.impl;

import dev.cheat.client.command.Command;
import dev.cheat.client.command.CommandException;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.setting.Setting;
import dev.cheat.client.util.ChatUtil;

import java.util.Arrays;

public final class SetCommand extends Command {
    public SetCommand() {
        super("set", "Changes a module setting, or lists them.", "<module> [setting] [value]", "s", "value");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length == 0) throw CommandException.usage();
        Module module = Modules.find(args[0]);
        if (args.length == 1) {
            if (module.getSettings().isEmpty()) {
                ChatUtil.info(module.getName() + " has no settings.");
                return;
            }
            for (Setting<?> setting : module.getSettings()) ChatUtil.info(setting.getName() + ": " + setting.display());
            return;
        }
        // Setting names may contain spaces: the last argument is the value, the rest is the name.
        Setting<?> shown = module.getSetting(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        if (shown != null) {
            ChatUtil.info(module.getName() + " " + shown.getName() + ": " + shown.display());
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
        Setting<?> setting = args.length < 3 ? null : module.getSetting(name);
        if (setting == null) {
            String wanted = args.length < 3 ? args[1] : name;
            throw new CommandException(module.getName() + " has no setting '" + wanted + "'.");
        }
        try {
            setting.parse(args[args.length - 1]);
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage());
        }
        ChatUtil.info(module.getName() + " " + setting.getName() + " set to " + setting.display() + ".");
    }
}
