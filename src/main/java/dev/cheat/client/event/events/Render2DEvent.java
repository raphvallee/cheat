package dev.cheat.client.event.events;

import dev.cheat.client.event.Event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Fired after the vanilla HUD is drawn (not when the HUD is hidden with F1). Draw with {@link #getGraphics()}
 * in GUI-scaled coordinates. Colors are ARGB, so remember the alpha byte (e.g. {@code 0xFFFFFFFF}).
 */
public final class Render2DEvent extends Event {
    private final GuiGraphicsExtractor graphics;
    private final DeltaTracker deltaTracker;

    public Render2DEvent(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        this.graphics = graphics;
        this.deltaTracker = deltaTracker;
    }

    public GuiGraphicsExtractor getGraphics() {
        return graphics;
    }

    public DeltaTracker getDeltaTracker() {
        return deltaTracker;
    }

    public float getPartialTicks() {
        return deltaTracker.getGameTimeDeltaPartialTick(true);
    }

    public int getWidth() {
        return graphics.guiWidth();
    }

    public int getHeight() {
        return graphics.guiHeight();
    }
}
