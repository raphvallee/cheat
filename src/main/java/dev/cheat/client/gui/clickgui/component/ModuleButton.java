package dev.cheat.client.gui.clickgui.component;

import dev.cheat.client.gui.clickgui.Theme;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.setting.BooleanSetting;
import dev.cheat.client.module.setting.ColorSetting;
import dev.cheat.client.module.setting.ModeSetting;
import dev.cheat.client.module.setting.NumberSetting;
import dev.cheat.client.module.setting.Setting;
import dev.cheat.client.util.ColorUtil;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** One module in a panel. Left click toggles it, right click shows its settings. */
public final class ModuleButton extends GuiElement {
    private final Module module;
    private final List<GuiElement> children = new ArrayList<>();
    private boolean expanded;

    public ModuleButton(Module module) {
        this.module = module;
        for (Setting<?> setting : module.getSettings()) {
            GuiElement element = createElement(setting);
            if (element != null) children.add(element);
        }
        children.add(new BindElement(module));
    }

    /** Maps a setting type to its GUI row. Add new setting types here. */
    private static GuiElement createElement(Setting<?> setting) {
        if (setting instanceof BooleanSetting s) return new BooleanElement(s);
        if (setting instanceof NumberSetting s) return new NumberElement(s);
        if (setting instanceof ModeSetting s) return new ModeElement(s);
        if (setting instanceof ColorSetting s) return new ColorElement(s);
        return null;
    }

    @Override
    public int height() {
        int height = Theme.MODULE_HEIGHT;
        if (expanded) {
            for (GuiElement child : children) {
                if (child.isVisible()) height += child.height();
            }
        }
        return height;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean hovered = RenderUtil.hovered(mouseX, mouseY, x, y, width, Theme.MODULE_HEIGHT);
        int accent = Theme.accent(y * 10L);
        RenderUtil.rect(g, x, y, width, Theme.MODULE_HEIGHT,
                module.isEnabled() ? ColorUtil.withAlpha(ColorUtil.darker(accent, 0.55f), 0xE0) : Theme.MODULE);
        if (hovered) RenderUtil.rect(g, x, y, width, Theme.MODULE_HEIGHT, Theme.HOVER);
        int textY = y + (Theme.MODULE_HEIGHT - RenderUtil.fontHeight()) / 2 + 1;
        RenderUtil.text(g, module.getName(), x + Theme.PADDING, textY, module.isEnabled() ? 0xFFFFFFFF : Theme.TEXT, false);
        if (children.size() > 1) {
            String arrow = expanded ? "-" : "+";
            RenderUtil.text(g, arrow, x + width - Theme.PADDING - RenderUtil.width(arrow), textY, Theme.TEXT_DIM, false);
        }
        if (!expanded) return;

        int childY = y + Theme.MODULE_HEIGHT;
        for (GuiElement child : children) {
            if (!child.isVisible()) continue;
            child.setBounds(x, childY, width);
            child.render(g, mouseX, mouseY);
            childY += child.height();
        }
        // Accent line marking the settings block.
        RenderUtil.rect(g, x, y + Theme.MODULE_HEIGHT, 1, childY - y - Theme.MODULE_HEIGHT, accent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (RenderUtil.hovered(mouseX, mouseY, x, y, width, Theme.MODULE_HEIGHT)) {
            if (button == LEFT) module.toggle();
            else if (button == RIGHT) expanded = !expanded;
            else return false;
            return true;
        }
        if (!expanded) return false;
        boolean used = false;
        for (GuiElement child : children) {
            // Every child gets the click so that e.g. a listening bind row can cancel when clicking elsewhere.
            if (child.isVisible() && child.mouseClicked(mouseX, mouseY, button)) used = true;
        }
        return used;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (GuiElement child : children) child.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key) {
        if (!expanded) return false;
        for (GuiElement child : children) {
            if (child.isVisible() && child.keyPressed(key)) return true;
        }
        return false;
    }

    @Override
    public String getDescription(double mouseX, double mouseY) {
        if (RenderUtil.hovered(mouseX, mouseY, x, y, width, Theme.MODULE_HEIGHT)) return module.getDescription();
        if (!expanded) return null;
        for (GuiElement child : children) {
            if (!child.isVisible()) continue;
            String description = child.getDescription(mouseX, mouseY);
            if (description != null) return description;
        }
        return null;
    }

    @Override
    public boolean charTyped(int codepoint) {
        if (!expanded) return false;
        for (GuiElement child : children) {
            if (child.isVisible() && child.charTyped(codepoint)) return true;
        }
        return false;
    }

    @Override
    public boolean isListening() {
        if (!expanded) return false;
        for (GuiElement child : children) {
            if (child.isVisible() && child.isListening()) return true;
        }
        return false;
    }
}
