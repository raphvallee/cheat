package dev.cheat.client.gui.clickgui.component;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cheat.client.gui.clickgui.Theme;
import dev.cheat.client.module.setting.NumberSetting;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Slider: click or drag to set, right click to reset to default.
 * A {@link NumberSetting#typed() typed} setting is a text field instead: click, type a number, Enter to apply.
 */
public final class NumberElement extends SettingElement<NumberSetting> {
    private static final int MAX_INPUT_LENGTH = 8;

    private boolean dragging;
    /**
     * Text being typed, or {@code null} when not editing.
     */
    private String input;

    public NumberElement(NumberSetting setting) {
        super(setting);
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    @Override
    public int height() {
        return setting.isTyped() ? Theme.ROW_HEIGHT : Theme.SLIDER_HEIGHT;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (setting.isTyped()) {
            background(g, mouseX, mouseY);
            boolean blink = System.currentTimeMillis() / 500 % 2 == 0;
            String value = input == null ? setting.display() : input + (blink ? "_" : " ");
            nameAndValue(g, value, input == null ? Theme.TEXT_DIM : Theme.accent());
            return;
        }
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
        if (!hovered(mouseX, mouseY)) {
            // Clicking elsewhere applies what was typed.
            commit();
            return false;
        }
        if (button == LEFT) {
            if (setting.isTyped()) {
                if (input == null) input = "";
                else commit();
            } else {
                dragging = true;
                update(mouseX);
            }
            return true;
        }
        if (button == RIGHT) {
            input = null;
            setting.reset();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == LEFT) dragging = false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (dragging) update(mouseX);
    }

    @Override
    public void defocus() {
        dragging = false;
        commit();
    }

    @Override
    public boolean isListening() {
        return input != null;
    }

    @Override
    public boolean keyPressed(int key) {
        if (input == null) return false;
        switch (key) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> commit();
            case InputConstants.KEY_ESCAPE -> input = null;
            case InputConstants.KEY_BACKSPACE -> {
                if (!input.isEmpty()) input = input.substring(0, input.length() - 1);
            }
            default -> {
                // Other keys arrive as characters through charTyped.
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(int codepoint) {
        if (input == null) return false;
        boolean digit = codepoint >= '0' && codepoint <= '9';
        boolean separator = (codepoint == '.' || codepoint == ',') && input.indexOf('.') < 0;
        boolean minus = codepoint == '-' && input.isEmpty() && setting.getMin() < 0;
        if ((digit || separator || minus) && input.length() < MAX_INPUT_LENGTH) {
            input += separator ? '.' : String.valueOf((char) codepoint);
        }
        return true;
    }

    /**
     * Applies the typed value (clamped to the setting's range); an empty or invalid entry keeps the old value.
     */
    private void commit() {
        if (input == null) return;
        String typed = input;
        input = null;
        if (typed.isEmpty() || typed.equals(".") || typed.equals("-") || typed.equals("-.")) return;
        try {
            setting.parse(typed);
        } catch (IllegalArgumentException ignored) {
            // Keep the previous value.
        }
    }

    @Override
    public String getDescription(double mouseX, double mouseY) {
        String description = super.getDescription(mouseX, mouseY);
        if (!setting.isTyped() || !hovered(mouseX, mouseY)) return description;
        String hint = "Click, type a number (" + format(setting.getMin()) + "-" + format(setting.getMax())
                + ") and press Enter. Right click to reset.";
        return description == null ? hint : description + " " + hint;
    }
}
