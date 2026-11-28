package dev.cheat.client.util;

/**
 * ARGB color helpers.
 */
public final class ColorUtil {
    private static final double RAINBOW_PERIOD = 4000;
    private static final long RAINBOW_CLOCK_WRAP = 400_000L;

    private ColorUtil() {
    }

    public static int argb(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha & 0xFF) << 24;
    }

    /**
     * @param hue degrees (0-360), saturation and brightness 0-1. Returns an opaque ARGB color.
     */
    public static int hsb(float hue, float saturation, float brightness) {
        float h = ((hue % 360f) + 360f) % 360f / 60f;
        float c = brightness * saturation;
        float x = c * (1 - Math.abs(h % 2 - 1));
        float m = brightness - c;
        float r, g, b;
        switch ((int) h) {
            case 0 -> {
                r = c;
                g = x;
                b = 0;
            }
            case 1 -> {
                r = x;
                g = c;
                b = 0;
            }
            case 2 -> {
                r = 0;
                g = c;
                b = x;
            }
            case 3 -> {
                r = 0;
                g = x;
                b = c;
            }
            case 4 -> {
                r = x;
                g = 0;
                b = c;
            }
            default -> {
                r = c;
                g = 0;
                b = x;
            }
        }
        return argb(255, Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }

    /**
     * Inverse of {@link #hsb}: returns {hue 0-360, saturation 0-1, brightness 0-1}, alpha ignored.
     */
    public static float[] toHsb(int argb) {
        float r = (argb >> 16 & 0xFF) / 255f, g = (argb >> 8 & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
        float hue;
        if (delta == 0) hue = 0;
        else if (max == r) hue = 60 * (((g - b) / delta) % 6);
        else if (max == g) hue = 60 * ((b - r) / delta + 2);
        else hue = 60 * ((r - g) / delta + 4);
        if (hue < 0) hue += 360;
        return new float[]{hue, max == 0 ? 0 : delta / max, max};
    }

    /**
     * Animated rainbow; {@code offset} shifts the phase (e.g. per line of a list).
     */
    public static int rainbow(long offsetMillis, float saturation, float brightness) {
        return rainbow(offsetMillis, 1, saturation, brightness);
    }

    public static int rainbow(long offsetMillis, double speed, float saturation, float brightness) {
        double time = (System.currentTimeMillis() % RAINBOW_CLOCK_WRAP) * speed;
        double phase = ((time + offsetMillis) % RAINBOW_PERIOD + RAINBOW_PERIOD) % RAINBOW_PERIOD;
        return hsb((float) (phase / RAINBOW_PERIOD * 360), saturation, brightness);
    }

    public static int darker(int color, float factor) {
        int a = color >>> 24, r = color >> 16 & 0xFF, g = color >> 8 & 0xFF, b = color & 0xFF;
        return argb(a, (int) (r * factor), (int) (g * factor), (int) (b * factor));
    }
}
