package dev.cheat.client.event.events;

import dev.cheat.client.event.Event;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Fired when joining a world or changing dimension ({@code level != null}) and when leaving it ({@code level == null}).
 */
public final class WorldChangeEvent extends Event {
    private final ClientLevel level;

    public WorldChangeEvent(ClientLevel level) {
        this.level = level;
    }

    /**
     * The new level, or {@code null} when leaving.
     */
    public ClientLevel getLevel() {
        return level;
    }
}
