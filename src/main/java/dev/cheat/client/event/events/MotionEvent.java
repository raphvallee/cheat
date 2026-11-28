package dev.cheat.client.event.events;

import dev.cheat.client.event.Event;

/**
 * Fired around {@code LocalPlayer.sendChanges()}, which sends the movement packet every tick (only a rotation
 * packet while riding).
 *
 * <p>In {@link State#PRE} the rotation and ground state can be changed: the server receives the new values
 * while the client camera keeps the real ones (silent rotations). Values are restored before {@link State#POST}.</p>
 */
public final class MotionEvent extends Event {
    public enum State { PRE, POST }

    private final State state;
    private final double x;
    private final double y;
    private final double z;
    private float yaw;
    private float pitch;
    private boolean onGround;

    public MotionEvent(State state, double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.state = state;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    public State getState() {
        return state;
    }

    public boolean isPre() {
        return state == State.PRE;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }
}
