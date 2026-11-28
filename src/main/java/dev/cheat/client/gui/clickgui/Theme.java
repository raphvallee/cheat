package dev.cheat.client.gui.clickgui;

import dev.cheat.client.module.impl.client.ClickGuiModule;

/**
 * ClickGUI colors and sizes in one place. Colors are ARGB.
 */
public final class Theme {
    public static final int PANEL_WIDTH = 112;
    public static final int HEADER_HEIGHT = 18;
    public static final int MODULE_HEIGHT = 15;
    public static final int ROW_HEIGHT = 13;
    public static final int SLIDER_HEIGHT = 17;
    public static final int PADDING = 4;

    public static final int HEADER = 0xF0101014;
    public static final int PANEL = 0xE01A1A20;
    public static final int MODULE = 0xE0202028;
    public static final int SETTINGS = 0xE0141418;
    public static final int HOVER = 0x18FFFFFF;
    public static final int TEXT = 0xFFE6E6E6;
    public static final int TEXT_DIM = 0xFF9A9AA4;
    public static final int SLIDER_TRACK = 0xFF2E2E38;
    public static final int TOOLTIP = 0xE0101014;

    private Theme() {
    }

    public static int accent() {
        return ClickGuiModule.accent(0);
    }

    public static int accent(long offset) {
        return ClickGuiModule.accent(offset);
    }
}
