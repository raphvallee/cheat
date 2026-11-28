package dev.cheat.client.event.events;

import dev.cheat.client.event.Event;
import net.minecraft.client.player.LocalPlayer;

public class PreTravelEvent extends Event {
    private final LocalPlayer player;

    public PreTravelEvent(LocalPlayer player) {
        this.player = player;
    }

    public LocalPlayer getPlayer() {
        return player;
    }
}