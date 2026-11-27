package dev.cheat.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public void toggle() {
        set(!get());
    }

    public boolean isEnabled() {
        return get();
    }

    @Override
    public void parse(String input) {
        switch (input.toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes", "1" -> set(true);
            case "false", "off", "no", "0" -> set(false);
            case "toggle" -> toggle();
            default -> throw new IllegalArgumentException("Expected true/false");
        }
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) set(json.getAsBoolean());
    }
}
