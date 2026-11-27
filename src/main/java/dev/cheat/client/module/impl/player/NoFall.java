package dev.cheat.client.module.impl.player;

import dev.cheat.client.event.Subscribe;
import dev.cheat.client.event.events.MotionEvent;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.setting.NumberSetting;

/** Example: changes what the movement packet says without moving the client ({@link MotionEvent}). */
public final class NoFall extends Module {
    private final NumberSetting minDistance = number("Min distance", 2.5, 1, 10, 0.5).withSuffix("m");

    public NoFall() {
        super("NoFall", "Tells the server you are on the ground while falling.", Category.PLAYER);
    }

    @Subscribe
    private void onMotion(MotionEvent event) {
        if (event.isPre() && mc.player.fallDistance >= minDistance.getDouble()) {
            event.setOnGround(true);
        }
    }

    @Override
    public String getTag() {
        return "Spoof";
    }
}
