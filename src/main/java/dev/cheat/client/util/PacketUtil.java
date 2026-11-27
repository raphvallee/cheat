package dev.cheat.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;

/** Packet helpers. */
public final class PacketUtil {
    private static final ThreadLocal<int[]> SILENT_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private PacketUtil() {}

    /** Sends a packet to the server, firing {@link dev.cheat.client.event.events.PacketSendEvent}. */
    public static void send(Packet<?> packet) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.send(packet);
    }

    /** Sends a packet without firing {@link dev.cheat.client.event.events.PacketSendEvent} (no module sees it). */
    public static void sendSilent(Packet<?> packet) {
        int[] depth = SILENT_DEPTH.get();
        depth[0]++;
        try {
            send(packet);
        } finally {
            depth[0]--;
        }
    }

    public static boolean isSilent() {
        return SILENT_DEPTH.get()[0] > 0;
    }
}
