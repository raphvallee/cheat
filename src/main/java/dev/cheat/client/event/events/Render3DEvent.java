package dev.cheat.client.event.events;

import dev.cheat.client.event.Event;

/**
 * Fired once per frame while in a world, right before the world render state is extracted.
 * Draw in-world with {@link net.minecraft.gizmos.Gizmos} (boxes, lines, arrows, text) in world coordinates;
 * see {@link dev.cheat.client.util.RenderUtil}. Use {@link #getPartialTicks()} to interpolate entity positions.
 */
public final class Render3DEvent extends Event {
    private final float partialTicks;

    public Render3DEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
