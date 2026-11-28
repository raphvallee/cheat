package dev.cheat.client.gui.clickgui.component;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A row inside a ClickGUI panel. The parent sets {@link #x}, {@link #y} and {@link #width} before each
 * {@link #render} call; input methods use the position from the last frame.
 */
public abstract class GuiElement {
    public static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
    public static final int RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;
    public static final int MIDDLE = InputConstants.MOUSE_BUTTON_MIDDLE;

    protected int x;
    protected int y;
    protected int width;

    public final void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public abstract int height();

    public abstract void render(GuiGraphicsExtractor g, int mouseX, int mouseY);

    /** @return true when the click was used */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public void mouseMoved(double mouseX, double mouseY) {
    }

    public void defocus() {
    }

    /** @return true when the key was used (e.g. while waiting for a bind) */
    public boolean keyPressed(int key) {
        return false;
    }

    /** @return true when the typed character was used (e.g. while editing a text field) */
    public boolean charTyped(int codepoint) {
        return false;
    }

    /** True while this row captures the keyboard (waiting for a bind, editing a value). */
    public boolean isListening() {
        return false;
    }

    public boolean isVisible() {
        return true;
    }

    /** Description shown at the bottom of the screen while hovered, or {@code null}. */
    public String getDescription(double mouseX, double mouseY) {
        return null;
    }

    protected boolean hovered(double mouseX, double mouseY) {
        return RenderUtil.hovered(mouseX, mouseY, x, y, width, height());
    }
}
