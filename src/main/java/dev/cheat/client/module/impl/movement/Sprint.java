package dev.cheat.client.module.impl.movement;

import dev.cheat.client.event.Subscribe;
import dev.cheat.client.event.events.PlayerTickEvent;
import dev.cheat.client.event.events.PreTravelEvent;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.setting.BooleanSetting;
import net.minecraft.client.player.LocalPlayer;

/**
 * Example: simplest possible module, reacting to {@link PlayerTickEvent}.
 */
public final class Sprint extends Module {
    private final BooleanSetting omniSprint = bool("Omni Sprint", false);
    private final BooleanSetting sideSprint = bool("Side Sprint", false).visibleIf(omniSprint::get);

    public Sprint() {
        super("Sprint", "Keeps the sprint key held down.", Category.MOVEMENT);
    }

    @Subscribe
    private void onPlayerTick(PlayerTickEvent event) {
        if (!omniSprint.get()) {
            mc.options.keySprint.setDown(true);
        }
    }

    @Subscribe
    private void onPreTravel(PreTravelEvent event) {
        LocalPlayer p = event.getPlayer();
        if (omniSprint.get() && p.input.getMoveVector().lengthSquared() > 0 && p.getFoodData().getFoodLevel() > 6 && !p.isShiftKeyDown()) {
            if (!sideSprint.get() && p.input.getMoveVector().x != 0) return;
            p.setSprinting(true);
        }
    }

    @Override
    protected void onDisable() {
        mc.options.keySprint.setDown(false);
    }
}
