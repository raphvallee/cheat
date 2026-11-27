package dev.cheat.client.event.events;

import dev.cheat.client.event.Event;
import net.minecraft.client.player.LocalPlayer;

/** Fired at the start of {@link LocalPlayer#tick()}, i.e. once per tick while in a world. The usual place for module logic. */
public final class PlayerTickEvent extends Event {
    private final LocalPlayer player;

    public PlayerTickEvent(LocalPlayer player) {
        this.player = player;
    }

    public LocalPlayer getPlayer() {
        return player;
    }
}
