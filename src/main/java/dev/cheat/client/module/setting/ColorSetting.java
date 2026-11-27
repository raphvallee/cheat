package dev.cheat.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

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

    /** Accepts {@code #RRGGBB} or {@code #AARRGGBB}. */
    @Override
    public void parse(String input) {
        String hex = input.startsWith("#") ? input.substring(1) : input;
        try {
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() == 6) set((int) (0xFF000000L | parsed));
            else if (hex.length() == 8) set((int) parsed);
            else throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected #RRGGBB or #AARRGGBB");
        }
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
