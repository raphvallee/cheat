package dev.cheat.client.gui.clickgui.component;

import dev.cheat.client.gui.clickgui.Theme;
import dev.cheat.client.module.setting.NumberSetting;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Slider: click or drag to set, right click to reset to default. */
public final class NumberElement extends SettingElement<NumberSetting> {
    private boolean dragging;

    public NumberElement(NumberSetting setting) {
        super(setting);
    }

    @Override
    public int height() {
        return Theme.SLIDER_HEIGHT;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (dragging) update(mouseX);
        background(g, mouseX, mouseY);
        nameAndValue(g, setting.display(), Theme.TEXT_DIM);
        int trackX = x + Theme.PADDING + 2;
        int trackWidth = width - Theme.PADDING * 2 - 2;
        int trackY = y + height() - 4;
        RenderUtil.rect(g, trackX, trackY, trackWidth, 2, Theme.SLIDER_TRACK);
        RenderUtil.rect(g, trackX, trackY, (int) Math.round(trackWidth * setting.getPercent()), 2, Theme.accent());
    }

    private void update(double mouseX) {
        int trackX = x + Theme.PADDING + 2;
        int trackWidth = width - Theme.PADDING * 2 - 2;
        setting.setPercent((mouseX - trackX) / trackWidth);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered(mouseX, mouseY)) return false;
        if (button == LEFT) {
            dragging = true;
            update(mouseX);
            return true;
        }
        if (button == RIGHT) {
            setting.reset();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == LEFT) dragging = false;
    }
}
