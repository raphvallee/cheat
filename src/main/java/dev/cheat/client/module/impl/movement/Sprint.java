package dev.cheat.client.module.impl.movement;

import dev.cheat.client.event.Subscribe;
import dev.cheat.client.event.events.PlayerTickEvent;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;

/** Example: simplest possible module, reacting to {@link PlayerTickEvent}. */
public final class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Keeps the sprint key held down.", Category.MOVEMENT);
    }

    @Subscribe
    private void onPlayerTick(PlayerTickEvent event) {
        mc.options.keySprint.setDown(true);
    }

    @Override
    protected void onDisable() {
        mc.options.keySprint.setDown(false);
    }
}
