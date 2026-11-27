package dev.cheat.client.event.events;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cheat.client.event.CancellableEvent;

/**
 * Fired for every keyboard key event, before vanilla handles it (also while a screen is open).
 * Cancel it to hide the key from vanilla.
 *
 * <p>{@link #getKey()} uses the same values as {@code InputConstants.KEY_*}; {@link #getAction()} is
 * {@link InputConstants#PRESS}, {@link InputConstants#RELEASE} or {@link InputConstants#REPEAT}.</p>
 */
public final class KeyInputEvent extends CancellableEvent {
    private final int key;
    private final int keycode;
    private final int action;
    private final int modifiers;
    private final boolean inScreen;

    public KeyInputEvent(int key, int keycode, int action, int modifiers, boolean inScreen) {
        this.key = key;
        this.keycode = keycode;
        this.action = action;
        this.modifiers = modifiers;
        this.inScreen = inScreen;
    }

    public int getKey() {
        return key;
    }

    public int getKeycode() {
        return keycode;
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

    /** Whether a screen (chat, inventory, ClickGUI, ...) was open when the key was pressed. */
    public boolean isInScreen() {
        return inScreen;
    }
}
