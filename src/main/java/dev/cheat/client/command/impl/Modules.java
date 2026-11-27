package dev.cheat.client.command.impl;

import dev.cheat.client.Client;
import dev.cheat.client.command.CommandException;
import dev.cheat.client.module.Module;

/** Shared argument parsing for module commands. */
final class Modules {
    private Modules() {}

    static Module find(String name) throws CommandException {
        Module module = Client.modules().getModule(name);
        if (module == null) throw new CommandException("Module '" + name + "' not found.");
        return module;
    }
}
