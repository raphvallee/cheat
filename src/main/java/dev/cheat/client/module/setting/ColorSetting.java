package dev.cheat.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;
import java.util.regex.Pattern;

/** An ARGB color, edited in the ClickGUI with hue/saturation/brightness/alpha sliders. */
public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, int defaultArgb) {
        super(name, defaultArgb);
    }

    public int getArgb() {
        return get();
    }

    public int getAlpha() {
        return get() >>> 24;
    }

    @Override
    public String display() {
        return String.format(Locale.ROOT, "#%08X", get());
    }

    private static final Pattern HEX = Pattern.compile("[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8}");

    /** Accepts {@code #RRGGBB} or {@code #AARRGGBB}. */
    @Override
    public void parse(String input) {
        String trimmed = input.trim();
        String hex = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
        if (!HEX.matcher(hex).matches()) throw new IllegalArgumentException("Expected #RRGGBB or #AARRGGBB");
        long parsed = Long.parseLong(hex, 16);
        set(hex.length() == 6 ? (int) (0xFF000000L | parsed) : (int) parsed);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(display());
    }

    @Override
    public void fromJson(JsonElement json) {
        try {
            if (json.isJsonPrimitive()) parse(json.getAsString());
        } catch (IllegalArgumentException ignored) {
        }
    }
}
