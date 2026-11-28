package dev.cheat.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/**
 * One choice out of a fixed list, e.g. {@code mode("Mode", "Packet", "Packet", "Vanilla")}.
 */
public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = List.of(modes);
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default mode " + defaultValue + " is not one of " + this.modes);
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return get().equalsIgnoreCase(mode);
    }

    public int index() {
        return modes.indexOf(get());
    }

    /**
     * Selects the next ({@code direction > 0}) or previous mode, wrapping around.
     */
    public void cycle(int direction) {
        int next = Math.floorMod(index() + Integer.signum(direction), modes.size());
        set(modes.get(next));
    }

    @Override
    protected String validate(String value) {
        for (String mode : modes) {
            if (mode.equalsIgnoreCase(value)) return mode;
        }
        return get() == null ? modes.getFirst() : get();
    }

    @Override
    public void parse(String input) {
        for (String mode : modes) {
            if (mode.equalsIgnoreCase(input)) {
                set(mode);
                return;
            }
        }
        throw new IllegalArgumentException("Expected one of " + String.join(", ", modes));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive()) set(json.getAsString());
    }
}
