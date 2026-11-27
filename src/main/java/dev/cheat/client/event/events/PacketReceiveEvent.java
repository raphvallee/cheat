package dev.cheat.client.event.events;

import dev.cheat.client.event.CancellableEvent;
import net.minecraft.network.protocol.Packet;

/**
 * Fired when a packet arrives from the server, before vanilla handles it. Cancel to drop it.
 *
 * <p><b>Threading:</b> posted on the Netty network thread. Do not touch the world here; read the packet and
 * schedule work with {@code Minecraft.getInstance().execute(...)} when needed.</p>
 */
public final class PacketReceiveEvent extends CancellableEvent {
    private final Packet<?> packet;

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}
