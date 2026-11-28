package dev.cheat.client.event.events;

import dev.cheat.client.event.Event;

/**
 * Fired at the start ({@link State#PRE}) and end ({@link State#POST}) of every client tick (20/s), in and out of game.
 */
public final class TickEvent extends Event {
    private final State state;

    public TickEvent(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public boolean isPre() {
        return state == State.PRE;
    }

    public enum State {PRE, POST}
}
