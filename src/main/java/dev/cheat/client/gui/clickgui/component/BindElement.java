package dev.cheat.client.gui.clickgui.component;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cheat.client.gui.clickgui.Theme;
import dev.cheat.client.module.Module;
import dev.cheat.client.util.KeyUtil;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Click, then press a key to bind. Escape / Backspace / Delete or right click unbind.
 */
public final class BindElement extends GuiElement {
    private final Module module;
    private boolean listening;

    public BindElement(Module module) {
        this.module = module;
    }

    @Override
    public boolean isListening() {
        return listening;
    }

    @Override
    public int height() {
        return Theme.ROW_HEIGHT;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        RenderUtil.rect(g, x, y, width, height(), Theme.SETTINGS);
        if (hovered(mouseX, mouseY)) RenderUtil.rect(g, x, y, width, height(), Theme.HOVER);
        int textY = y + (height() - RenderUtil.fontHeight()) / 2 + 1;
        RenderUtil.text(g, "Bind", x + Theme.PADDING + 2, textY, Theme.TEXT, false);
        String value = listening ? "..." : KeyUtil.name(module.getKey());
        RenderUtil.text(g, value, x + width - Theme.PADDING - RenderUtil.width(value), textY,
                listening ? Theme.accent() : Theme.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered(mouseX, mouseY)) {
            listening = false;
            return false;
        }
        if (button == LEFT) listening = !listening;
        else if (button == RIGHT) {
            module.setKey(KeyUtil.NONE);
            listening = false;
        } else return false;
        return true;
    }

    @Override
    public void defocus() {
        listening = false;
    }

    @Override
    public boolean keyPressed(int key) {
        if (!listening) return false;
        boolean clear = key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE;
        module.setKey(clear ? KeyUtil.NONE : key);
        listening = false;
        return true;
    }

    @Override
    public String getDescription(double mouseX, double mouseY) {
        return hovered(mouseX, mouseY) ? "Click then press a key. Right click / Esc to unbind." : null;
    }
}
