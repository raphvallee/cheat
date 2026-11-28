package dev.cheat.client.module.impl.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.cheat.client.Client;
import dev.cheat.client.gui.clickgui.ClickGuiScreen;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.setting.BooleanSetting;
import dev.cheat.client.module.setting.ColorSetting;
import dev.cheat.client.module.setting.NumberSetting;
import dev.cheat.client.util.ColorUtil;

import java.util.EnumMap;
import java.util.Map;

/**
 * Opens the ClickGUI (Right Shift by default) and holds GUI-wide settings such as the accent color.
 */
public final class ClickGuiModule extends Module {
    public final ColorSetting accent = color("Accent", 0xFF4FA3FF);
    public final BooleanSetting rainbow = bool("Rainbow", false);
    public final NumberSetting rainbowSpeed = number("Rainbow speed", 1, 0.1, 5, 0.1).visibleIf(rainbow::isEnabled);
    public final NumberSetting scale = number("Scale", 1, 0.5, 2, 0.05).typed()
                                                                       .describedAs("Size of the GUI; stays the same whatever the game's GUI scale is. Capped to fit the window.");
    public final BooleanSetting blur = bool("Blur", false);
    public final BooleanSetting descriptions = bool("Descriptions", true);
    private final Map<Category, PanelState> panels = new EnumMap<>(Category.class);

    public ClickGuiModule() {
        super("ClickGUI", "Opens this menu.", Category.CLIENT, InputConstants.KEY_RSHIFT);
        setHidden(true);
        int x = 6;
        for (Category category : Category.values()) {
            panels.put(category, new PanelState(x, 6));
            x += 118;
        }
    }

    /**
     * Accent color of the running client (works from anywhere after startup).
     */
    public static int accent(long offset) {
        return Client.modules().getModule(ClickGuiModule.class).accentColor(offset);
    }

    @Override
    protected void onEnable() {
        // Acts like a button: open the screen and switch straight back off.
        if (mc.gui.screen() == null) {
            mc.gui.setScreen(new ClickGuiScreen(this));
        } else {
            mc.schedule(() -> {
                if (mc.gui.screen() == null) mc.gui.setScreen(new ClickGuiScreen(this));
            });
        }
        setEnabled(false);
    }

    public PanelState panel(Category category) {
        return panels.get(category);
    }

    /**
     * Accent color for GUI and HUD; {@code offset} shifts the rainbow phase.
     */
    public int accentColor(long offset) {
        if (!rainbow.isEnabled()) return accent.getArgb();
        return ColorUtil.rainbow(offset, rainbowSpeed.getDouble(), 0.6f, 1f);
    }

    public JsonObject panelsToJson() {
        JsonObject json = new JsonObject();
        panels.forEach((category, state) -> {
            JsonObject p = new JsonObject();
            p.addProperty("x", state.x);
            p.addProperty("y", state.y);
            p.addProperty("open", state.open);
            json.add(category.name(), p);
        });
        return json;
    }

    public void panelsFromJson(JsonObject json) {
        for (Map.Entry<String, JsonElement> e : json.entrySet()) {
            try {
                PanelState state = panels.get(Category.valueOf(e.getKey()));
                JsonObject p = e.getValue().getAsJsonObject();
                state.x = p.get("x").getAsInt();
                state.y = p.get("y").getAsInt();
                state.open = p.get("open").getAsBoolean();
            } catch (RuntimeException ignored) {
                // Unknown category or malformed entry: keep defaults.
            }
        }
    }

    /**
     * Saved position and state of each category panel.
     */
    public static final class PanelState {
        public int x;
        public int y;
        public boolean open = true;

        PanelState(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
