package dev.cheat.client.gui.clickgui.component;

import dev.cheat.client.gui.clickgui.Theme;
import dev.cheat.client.module.setting.ModeSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Left click: next mode. Right click: previous mode. */
public final class ModeElement extends SettingElement<ModeSetting> {
    public ModeElement(ModeSetting setting) {
        super(setting);
    }

    @Override
    public int height() {
        return Theme.ROW_HEIGHT;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        background(g, mouseX, mouseY);
        nameAndValue(g, setting.get(), Theme.accent());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered(mouseX, mouseY)) return false;
        if (button == LEFT) setting.cycle(1);
        else if (button == RIGHT) setting.cycle(-1);
        else return false;
        return true;
    }
}
