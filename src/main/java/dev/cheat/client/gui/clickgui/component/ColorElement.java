package dev.cheat.client.gui.clickgui.component;

import dev.cheat.client.gui.clickgui.Theme;
import dev.cheat.client.module.setting.ColorSetting;
import dev.cheat.client.util.ColorUtil;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Color row; click to expand hue / saturation / brightness / alpha sliders. Right click resets. */
public final class ColorElement extends SettingElement<ColorSetting> {
    private static final int SLIDER_ROW = 10;
    private static final String[] LABELS = {"H", "S", "B", "A"};

    private boolean expanded;
    private int dragging = -1;
    private final float[] hsba = new float[4];
    private int lastSynced;

    public ColorElement(ColorSetting setting) {
        super(setting);
        sync();
    }

    private void sync() {
        int argb = setting.getArgb();
        float[] hsb = ColorUtil.toHsb(argb);
        // Keep the hue while the color is gray/black, where it cannot be recovered from RGB.
        if (hsb[1] > 0 && hsb[2] > 0) hsba[0] = hsb[0] / 360f;
        hsba[1] = hsb[1];
        hsba[2] = hsb[2];
        hsba[3] = (argb >>> 24) / 255f;
        lastSynced = argb;
    }

    @Override
    public int height() {
        return Theme.ROW_HEIGHT + (expanded ? SLIDER_ROW * 4 + 2 : 0);
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (setting.getArgb() != lastSynced) sync();
        if (dragging >= 0) update(dragging, mouseX);

        RenderUtil.rect(g, x, y, width, height(), Theme.SETTINGS);
        if (RenderUtil.hovered(mouseX, mouseY, x, y, width, Theme.ROW_HEIGHT)) {
            RenderUtil.rect(g, x, y, width, Theme.ROW_HEIGHT, Theme.HOVER);
        }
        nameAndValue(g, null, 0);
        int swatch = 12;
        RenderUtil.rect(g, x + width - Theme.PADDING - swatch, y + 3, swatch, Theme.ROW_HEIGHT - 6, setting.getArgb() | 0xFF000000);
        if (!expanded) return;

        for (int i = 0; i < 4; i++) {
            int rowY = y + Theme.ROW_HEIGHT + i * SLIDER_ROW;
            RenderUtil.text(g, LABELS[i], x + Theme.PADDING + 2, rowY + 1, Theme.TEXT_DIM, false);
            int trackX = trackX();
            int trackWidth = trackWidth();
            int trackY = rowY + 4;
            if (i == 0) {
                int segments = 24;
                for (int s = 0; s < segments; s++) {
                    int sx = trackX + trackWidth * s / segments;
                    int ex = trackX + trackWidth * (s + 1) / segments;
                    RenderUtil.rect(g, sx, trackY, ex - sx, 3, ColorUtil.hsb(360f * s / segments, 1, 1));
                }
            } else {
                RenderUtil.rect(g, trackX, trackY, trackWidth, 3, Theme.SLIDER_TRACK);
                RenderUtil.rect(g, trackX, trackY, Math.round(trackWidth * hsba[i]), 3, setting.getArgb() | 0xFF000000);
            }
            int knob = trackX + Math.round(trackWidth * hsba[i]);
            RenderUtil.rect(g, knob - 1, trackY - 1, 2, 5, 0xFFFFFFFF);
        }
    }

    private int trackX() {
        return x + Theme.PADDING + 10;
    }

    private int trackWidth() {
        return width - Theme.PADDING * 2 - 12;
    }

    private void update(int index, double mouseX) {
        hsba[index] = (float) Math.max(0, Math.min(1, (mouseX - trackX()) / trackWidth()));
        int rgb = ColorUtil.hsb(hsba[0] * 360f, hsba[1], hsba[2]);
        int argb = ColorUtil.withAlpha(rgb, Math.round(hsba[3] * 255));
        setting.set(argb);
        lastSynced = argb;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!hovered(mouseX, mouseY)) return false;
        if (mouseY < y + Theme.ROW_HEIGHT) {
            if (button == LEFT) expanded = !expanded;
            else if (button == RIGHT) setting.reset();
            else return false;
            return true;
        }
        if (button != LEFT) return false;
        int index = (int) ((mouseY - y - Theme.ROW_HEIGHT) / SLIDER_ROW);
        if (index < 0 || index > 3) return false;
        dragging = index;
        update(index, mouseX);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == LEFT) dragging = -1;
    }
}
