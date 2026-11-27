package dev.cheat.client.event;

/** An event whose vanilla behavior is skipped when cancelled. */
public abstract class CancellableEvent extends Event {
    private boolean cancelled;

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
