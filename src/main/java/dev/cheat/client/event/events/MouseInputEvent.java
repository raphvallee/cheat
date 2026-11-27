package dev.cheat.client.event.events;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cheat.client.event.CancellableEvent;

/**
 * Fired for every mouse button press/release before vanilla handles it. Cancel it to hide the click from vanilla.
 * Buttons use {@code InputConstants.MOUSE_BUTTON_*} (left = 1, middle = 2, right = 3).
 */
public final class MouseInputEvent extends CancellableEvent {
    private final int button;
    private final int action;
    private final int modifiers;
    private final boolean inScreen;

    public MouseInputEvent(int button, int action, int modifiers, boolean inScreen) {
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
        this.inScreen = inScreen;
    }

    public int getButton() {
        return button;
    }

    public int getAction() {
        return action;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isPress() {
        return action == InputConstants.PRESS;
    }

    public boolean isInScreen() {
        return inScreen;
    }
}
