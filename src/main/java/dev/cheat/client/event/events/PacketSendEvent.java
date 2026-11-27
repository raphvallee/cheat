package dev.cheat.client.event.events;

import dev.cheat.client.event.CancellableEvent;
import net.minecraft.network.protocol.Packet;

/**
 * Fired before the client sends a packet to the server. Cancel to drop it. Usually posted on the client thread.
 * Packets sent with {@link dev.cheat.client.util.PacketUtil#sendSilent} skip this event.
 */
public final class PacketSendEvent extends CancellableEvent {
    private final Packet<?> packet;

    public PacketSendEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}
