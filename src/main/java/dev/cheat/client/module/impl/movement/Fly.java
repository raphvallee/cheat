package dev.cheat.client.module.impl.movement;

import dev.cheat.client.event.Subscribe;
import dev.cheat.client.event.events.PlayerTickEvent;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.setting.ModeSetting;
import dev.cheat.client.module.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;

public final class Fly extends Module {

    enum FlyMode {
        Creative("Creative"), Jetpack("JetPack");

        final String name;

        FlyMode(String name) {
            this.name = name;
        }
    }

    private final ModeSetting mode = mode("Mode", FlyMode.Creative.name, FlyMode.Creative.name, FlyMode.Jetpack.name);
    private final NumberSetting jumpForce = number("Jump Force", 0.17, 0, 0.5, 0.01).visibleIf(() -> mode.is(FlyMode.Jetpack.name));
    private final NumberSetting maxVerticalSpeed = number("Max Speed", 15, 0, 50, 0.5).visibleIf(() -> mode.is(FlyMode.Jetpack.name));

    public Fly() {
        super("Fly", "Makes you fly like a bird", Category.MOVEMENT);
    }

    @Subscribe
    private void onPlayerTick(PlayerTickEvent event) {
        assert mc.player != null;
        if (mode.is(FlyMode.Creative.name)) {
            mc.player.getAbilities().flying = true;
        } else if (mode.is(FlyMode.Jetpack.name)) {
            if (mc.options.keyJump.isDown()) {
                Vec3 motion = mc.player.getDeltaMovement();
                double maxPerTick = maxVerticalSpeed.getDouble() / 20.0;
                double newY = Math.min(motion.y + jumpForce.getDouble(), maxPerTick);
                if (newY > motion.y) {
                    mc.player.setDeltaMovement(motion.x, newY, motion.z);
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        assert mc.player != null;
        if (mode.is(FlyMode.Creative.name)) {
            mc.player.getAbilities().flying = false;
        }
    }
}
