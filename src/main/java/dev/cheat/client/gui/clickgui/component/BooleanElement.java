package dev.cheat.client.gui.clickgui.component;

import dev.cheat.client.gui.clickgui.Theme;
import dev.cheat.client.module.setting.BooleanSetting;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BooleanElement extends SettingElement<BooleanSetting> {
    public BooleanElement(BooleanSetting setting) {
        super(setting);
    }

    @Override
    public int height() {
        return Theme.ROW_HEIGHT;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        background(g, mouseX, mouseY);
        nameAndValue(g, null, 0);
        int size = 7;
        int boxX = x + width - Theme.PADDING - size;
        int boxY = y + (height() - size) / 2;
        RenderUtil.rect(g, boxX, boxY, size, size, Theme.SLIDER_TRACK);
        if (setting.isEnabled()) RenderUtil.rect(g, boxX + 1, boxY + 1, size - 2, size - 2, Theme.accent());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered(mouseX, mouseY) || button != LEFT) return false;
        setting.toggle();
        return true;
    }
}
