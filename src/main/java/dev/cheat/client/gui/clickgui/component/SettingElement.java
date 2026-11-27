package dev.cheat.client.gui.clickgui.component;

import dev.cheat.client.gui.clickgui.Theme;
import dev.cheat.client.module.setting.Setting;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Base for rows editing one {@link Setting}. Hidden when the setting's {@code visibleIf} is false. */
public abstract class SettingElement<S extends Setting<?>> extends GuiElement {
    protected final S setting;

    protected SettingElement(S setting) {
        this.setting = setting;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }

    @Override
    public String getDescription(double mouseX, double mouseY) {
        return hovered(mouseX, mouseY) && !setting.getDescription().isEmpty() ? setting.getDescription() : null;
    }

    protected void background(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        RenderUtil.rect(g, x, y, width, height(), Theme.SETTINGS);
        if (hovered(mouseX, mouseY)) RenderUtil.rect(g, x, y, width, height(), Theme.HOVER);
    }

    /** Draws the setting name on the left and {@code value} right-aligned. */
    protected void nameAndValue(GuiGraphicsExtractor g, String value, int valueColor) {
        int textY = y + (Theme.ROW_HEIGHT - RenderUtil.fontHeight()) / 2 + 1;
        RenderUtil.text(g, setting.getName(), x + Theme.PADDING + 2, textY, Theme.TEXT, false);
        if (value != null) {
            RenderUtil.text(g, value, x + width - Theme.PADDING - RenderUtil.width(value), textY, valueColor, false);
        }
    }
}
