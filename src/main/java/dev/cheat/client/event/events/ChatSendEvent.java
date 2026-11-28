package dev.cheat.client.event.events;

import dev.cheat.client.event.CancellableEvent;

/**
 * Fired when the player submits a chat message or /command from the chat screen. Cancel to not send it.
 */
public final class ChatSendEvent extends CancellableEvent {
    private final String message;

    public ChatSendEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
