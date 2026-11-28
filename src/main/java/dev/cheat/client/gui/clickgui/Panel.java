package dev.cheat.client.gui.clickgui;

import dev.cheat.client.gui.clickgui.component.GuiElement;
import dev.cheat.client.gui.clickgui.component.ModuleButton;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.impl.client.ClickGuiModule;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * A draggable category window. Header: left drag to move, right click to collapse.
 * The module list scrolls with the mouse wheel when it is taller than the screen.
 */
public final class Panel {
    private final Category category;
    private final ClickGuiModule.PanelState state;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private int scroll;
    private int visibleHeight;
    private int[] placed;

    public Panel(Category category, ClickGuiModule.PanelState state, List<Module> modules) {
        this.category = category;
        this.state = state;
        for (Module module : modules) buttons.add(new ModuleButton(module));
    }

    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hover, int screenWidth, int screenHeight) {
        if (dragging) {
            state.x = Math.max(0, Math.min(screenWidth - Theme.PANEL_WIDTH, mouseX - dragOffsetX));
            state.y = Math.max(0, Math.min(screenHeight - Theme.HEADER_HEIGHT, mouseY - dragOffsetY));
        }
        int x = x();
        int y = y();
        int w = Theme.PANEL_WIDTH;

        RenderUtil.rect(g, x, y, w, Theme.HEADER_HEIGHT, Theme.HEADER);
        RenderUtil.rect(g, x, y + Theme.HEADER_HEIGHT - 1, w, 1, Theme.accent(x * 10L));
        int textY = y + (Theme.HEADER_HEIGHT - RenderUtil.fontHeight()) / 2 + 1;
        RenderUtil.text(g, category.getDisplayName(), x + Theme.PADDING, textY, Theme.TEXT, true);
        String count = String.valueOf(buttons.size());
        RenderUtil.text(g, count, x + w - Theme.PADDING - RenderUtil.width(count), textY, Theme.TEXT_DIM, false);
        if (!state.open) return;

        int contentHeight = 0;
        for (ModuleButton button : buttons) contentHeight += button.height();
        int top = y + Theme.HEADER_HEIGHT;
        int maxHeight = Math.max(Theme.MODULE_HEIGHT * 3, screenHeight - top - 4);
        visibleHeight = Math.min(contentHeight, maxHeight);
        scroll = Math.max(0, Math.min(scroll, contentHeight - visibleHeight));

        RenderUtil.rect(g, x, top, w, visibleHeight, Theme.PANEL);
        g.enableScissor(x, top, x + w, top + visibleHeight);
        int rowY = top - scroll;
        // Hide the hover state for rows scrolled out of view or covered by another panel.
        boolean mouseInside = hover && RenderUtil.hovered(mouseX, mouseY, x, top, w, visibleHeight);
        int mx = mouseInside ? mouseX : Integer.MIN_VALUE;
        int my = mouseInside ? mouseY : Integer.MIN_VALUE;
        for (ModuleButton button : buttons) {
            button.setBounds(x, rowY, w);
            button.render(g, mx, my);
            rowY += button.height();
        }
        g.disableScissor();
        RenderUtil.rect(g, x, top + visibleHeight, w, 1, Theme.HEADER);
    }

    public int x() {
        return placed != null ? placed[0] : state.x;
    }

    public int y() {
        return placed != null ? placed[1] : state.y;
    }

    public int savedX() {
        return state.x;
    }

    public int savedY() {
        return state.y;
    }

    public void placeAt(int x, int y) {
        placed = new int[]{x, y};
    }

    public void clearPlacement() {
        placed = null;
    }

    public boolean isOverHeader(double mouseX, double mouseY) {
        return RenderUtil.hovered(mouseX, mouseY, x(), y(), Theme.PANEL_WIDTH, Theme.HEADER_HEIGHT);
    }

    public boolean isOverContent(double mouseX, double mouseY) {
        return state.open && RenderUtil.hovered(mouseX, mouseY, x(), y() + Theme.HEADER_HEIGHT,
                Theme.PANEL_WIDTH, visibleHeight);
    }

    public boolean isOver(double mouseX, double mouseY) {
        return isOverHeader(mouseX, mouseY) || isOverContent(mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOverHeader(mouseX, mouseY)) {
            defocus();
            if (button == GuiElement.LEFT) {
                state.x = x();
                state.y = y();
                placed = null;
                dragging = true;
                dragOffsetX = (int) mouseX - state.x;
                dragOffsetY = (int) mouseY - state.y;
            } else if (button == GuiElement.RIGHT) {
                state.open = !state.open;
            }
            return true;
        }
        if (!state.open) {
            defocus();
            return false;
        }
        boolean inside = isOverContent(mouseX, mouseY);
        boolean used = false;
        for (ModuleButton moduleButton : buttons) {
            // Clicks outside the visible area still reach rows (with an impossible position) so they can reset state.
            if (moduleButton.mouseClicked(inside ? mouseX : Double.NaN, inside ? mouseY : Double.NaN, button))
                used = true;
        }
        return used || inside;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GuiElement.LEFT) dragging = false;
        for (ModuleButton moduleButton : buttons) moduleButton.mouseReleased(mouseX, mouseY, button);
    }

    public void mouseMoved(double mouseX, double mouseY) {
        for (ModuleButton button : buttons) button.mouseMoved(mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isOverContent(mouseX, mouseY)) return false;
        scroll -= (int) Math.round(amount * 14);
        return true;
    }

    public boolean keyPressed(int key) {
        if (!state.open) return false;
        for (ModuleButton button : buttons) {
            if (button.keyPressed(key)) return true;
        }
        return false;
    }

    public boolean charTyped(int codepoint) {
        if (!state.open) return false;
        for (ModuleButton button : buttons) {
            if (button.charTyped(codepoint)) return true;
        }
        return false;
    }

    public boolean isListening() {
        if (!state.open) return false;
        for (ModuleButton button : buttons) {
            if (button.isListening()) return true;
        }
        return false;
    }

    public void defocus() {
        for (ModuleButton button : buttons) button.defocus();
    }

    public String getDescription(double mouseX, double mouseY) {
        if (!isOverContent(mouseX, mouseY)) return null;
        for (ModuleButton button : buttons) {
            String description = button.getDescription(mouseX, mouseY);
            if (description != null) return description;
        }
        return null;
    }
}
