package dev.cheat.client.module.impl.combat;

import dev.cheat.client.event.Subscribe;
import dev.cheat.client.event.events.PacketReceiveEvent;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.setting.ModeSetting;
import dev.cheat.client.module.setting.NumberSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

/**
 * Example: incoming packet handling ({@link PacketReceiveEvent}, Netty thread), mode settings and
 * settings that only show for one mode ({@code visibleIf}).
 */
public final class Velocity extends Module {
    private final ModeSetting mode = mode("Mode", "Cancel", "Cancel", "Modify");
    private final NumberSetting horizontal = number("Horizontal", 0, 0, 100, 1).withSuffix("%")
            .visibleIf(() -> mode.is("Modify"));
    private final NumberSetting vertical = number("Vertical", 0, 0, 100, 1).withSuffix("%")
            .visibleIf(() -> mode.is("Modify"));

    public Velocity() {
        super("Velocity", "Reduces knockback.", Category.COMBAT);
    }

    @Subscribe
    private void onPacket(PacketReceiveEvent event) {
        LocalPlayer player = mc.player;
        if (player == null || !(event.getPacket() instanceof ClientboundSetEntityMotionPacket packet)
                || packet.id() != player.getId()) {
            return;
        }
        event.cancel();
        if (mode.is("Modify")) {
            Vec3 motion = packet.movement();
            double h = horizontal.getDouble() / 100.0;
            double v = vertical.getDouble() / 100.0;
            // Packet events run on the network thread: touch the player on the client thread.
            mc.execute(() -> player.setDeltaMovement(motion.x * h, motion.y * v, motion.z * h));
        }
    }

    @Override
    public String getTag() {
        return mode.is("Modify") ? horizontal.getInt() + "% " + vertical.getInt() + "%" : mode.get();
    }
}
