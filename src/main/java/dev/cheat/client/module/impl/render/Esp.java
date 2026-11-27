package dev.cheat.client.module.impl.render;

import dev.cheat.client.event.Subscribe;
import dev.cheat.client.event.events.Render3DEvent;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.setting.BooleanSetting;
import dev.cheat.client.module.setting.ColorSetting;
import dev.cheat.client.module.setting.NumberSetting;
import dev.cheat.client.util.ColorUtil;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Example: in-world rendering with {@link Render3DEvent} and Gizmos. */
public final class Esp extends Module {
    private final BooleanSetting players = bool("Players", true);
    private final BooleanSetting mobs = bool("Mobs", false);
    private final BooleanSetting items = bool("Items", false);
    private final ColorSetting color = color("Color", 0xFF55AAFF);
    private final NumberSetting lineWidth = number("Line width", 2, 1, 6, 0.5);
    private final BooleanSetting fill = bool("Fill", false);
    private final NumberSetting fillAlpha = number("Fill alpha", 40, 5, 255, 5).visibleIf(fill::isEnabled);

    public Esp() {
        super("ESP", "Draws boxes around entities.", Category.RENDER);
    }

    @Subscribe
    private void onRender3D(Render3DEvent event) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !shouldRender(entity)) continue;
            AABB box = RenderUtil.interpolatedBox(entity, event.getPartialTicks());
            if (fill.isEnabled()) {
                RenderUtil.filledBox(box, color.getArgb(), lineWidth.getFloat(),
                        ColorUtil.withAlpha(color.getArgb(), fillAlpha.getInt()));
            } else {
                RenderUtil.box(box, color.getArgb(), lineWidth.getFloat());
            }
        }
    }

    private boolean shouldRender(Entity entity) {
        if (entity instanceof Player) return players.isEnabled();
        if (entity instanceof Mob) return mobs.isEnabled();
        if (entity instanceof ItemEntity) return items.isEnabled();
        return false;
    }
}
