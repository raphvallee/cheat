package dev.cheat.client.gui.clickgui;

import dev.cheat.client.Client;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.impl.client.ClickGuiModule;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LiquidBounce-style ClickGUI: one draggable panel per category.
 * Left click a module to toggle it, right click it to edit its settings and key bind.
 *
 * <p>Everything is drawn in a virtual coordinate space scaled by {@link #scale()}, so the GUI keeps the same
 * size whatever the game's GUI scale is. Mouse positions are converted with {@link #toVirtual}.</p>
 */
public final class ClickGuiScreen extends Screen {
    private static final int REFERENCE_GUI_SCALE = 2;

    private final ClickGuiModule module;
    private final List<Panel> panels = new ArrayList<>();

    public ClickGuiScreen(ClickGuiModule module) {
        super(Component.literal(Client.NAME + " ClickGUI"));
        this.module = module;
        for (Category category : Category.values()) {
            List<Module> modules = Client.modules().getModules(category);
            if (!modules.isEmpty()) panels.add(new Panel(category, module.panel(category), modules));
        }
    }

    private float scale() {
        return (float) (module.scale.getDouble() * REFERENCE_GUI_SCALE / minecraft.getWindow().getGuiScale());
    }

    private double toVirtual(double screenCoordinate) {
        return screenCoordinate / scale();
    }

    private int virtualWidth() {
        return (int) (width / scale());
    }

    private int virtualHeight() {
        return (int) (height / scale());
    }

    @Override
    protected void init() {
        // If a saved/default layout does not fit this window, flow the panels into rows that do.
        int vw = virtualWidth();
        int vh = virtualHeight();
        boolean outside = panels.stream().anyMatch(p -> p.x() + Theme.PANEL_WIDTH > vw || p.y() + Theme.HEADER_HEIGHT > vh);
        if (!outside) return;
        int x = 6;
        int y = 6;
        for (Panel panel : panels) {
            if (x + Theme.PANEL_WIDTH > vw) {
                x = 6;
                y += Theme.HEADER_HEIGHT + Theme.MODULE_HEIGHT * 3 + 10;
            }
            panel.moveTo(x, Math.min(y, Math.max(0, vh - Theme.HEADER_HEIGHT)));
            x += Theme.PANEL_WIDTH + 6;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (module.blur.isEnabled()) extractBlurredBackground(g);
        extractTransparentBackground(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        float scale = scale();
        int mx = (int) toVirtual(mouseX);
        int my = (int) toVirtual(mouseY);
        int vw = virtualWidth();
        int vh = virtualHeight();
        g.pose().pushMatrix();
        g.pose().scale(scale, scale);

        for (Panel panel : panels) panel.render(g, mx, my, vw, vh);

        if (module.descriptions.isEnabled()) {
            String description = null;
            for (int i = panels.size() - 1; i >= 0 && description == null; i--) {
                Panel panel = panels.get(i);
                if (panel.isOver(mx, my)) description = panel.getDescription(mx, my);
            }
            if (description != null && !description.isEmpty()) {
                int w = RenderUtil.width(description) + 8;
                int y = vh - RenderUtil.fontHeight() - 10;
                RenderUtil.rect(g, 4, y, w, RenderUtil.fontHeight() + 6, Theme.TOOLTIP);
                RenderUtil.rect(g, 4, y, 1, RenderUtil.fontHeight() + 6, Theme.accent());
                RenderUtil.text(g, description, 8, y + 3, Theme.TEXT, false);
            }
        }
        String footer = Client.NAME + " " + Client.VERSION;
        RenderUtil.text(g, footer, vw - RenderUtil.width(footer) - 4, vh - RenderUtil.fontHeight() - 4, Theme.TEXT_DIM, true);

        g.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = toVirtual(event.x());
        double my = toVirtual(event.y());
        // Topmost panel first; the clicked panel moves to the front.
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (panel.isOver(mx, my)) {
                panel.mouseClicked(mx, my, event.button());
                panels.remove(i);
                panels.add(panel);
                // Let the other panels reset transient state (e.g. stop waiting for a key bind).
                for (Panel other : panels) {
                    if (other != panel) other.mouseClicked(Double.NaN, Double.NaN, event.button());
                }
                return true;
            }
        }
        for (Panel panel : panels) panel.mouseClicked(Double.NaN, Double.NaN, event.button());
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (Panel panel : panels) panel.mouseReleased(toVirtual(event.x()), toVirtual(event.y()), event.button());
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double mx = toVirtual(mouseX);
        double my = toVirtual(mouseY);
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseScrolled(mx, my, scrollY)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // While a bind row waits for a key, every key (including Escape) goes to it.
        for (Panel panel : panels) {
            if (panel.isListening()) {
                panel.keyPressed(event.key());
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        Client.config().save();
        super.removed();
    }
}
