package dev.cheat.client.module;

import dev.cheat.client.Client;
import dev.cheat.client.module.setting.*;
import dev.cheat.client.util.KeyUtil;
import dev.cheat.client.util.MinecraftInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class of every module.
 *
 * <pre>{@code
 * public final class Sprint extends Module {
 *     private final BooleanSetting omni = bool("Omni", false);
 *
 *     public Sprint() {
 *         super("Sprint", "Sprints automatically.", Category.MOVEMENT);
 *     }
 *
 *     @Subscribe
 *     private void onTick(PlayerTickEvent event) { ... }
 * }
 * }</pre>
 * <p>
 * Register it in {@link ModuleManager#registerDefaults()}. While enabled, the module's {@code @Subscribe}
 * methods receive events. {@link #onEnable()} / {@link #onDisable()} may run outside of a world (for example
 * when the config loads at startup), so null-check {@code mc.player} / {@code mc.level}.
 */
public abstract class Module implements MinecraftInstance {
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private int key;
    private boolean enabled;
    private boolean hidden;
    private boolean enabledByDefault;
    private int defaultKey;
    private boolean hiddenByDefault;

    protected Module(String name, String description, Category category) {
        this(name, description, category, KeyUtil.NONE);
    }

    protected Module(String name, String description, Category category, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = defaultKey;
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Called when the module is turned on, after it was subscribed to events.
     */
    protected void onEnable() {
    }

    /**
     * Called when the module is turned off, after it was unsubscribed from events.
     */
    protected void onDisable() {
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    protected final BooleanSetting bool(String name, boolean defaultValue) {
        return addSetting(new BooleanSetting(name, defaultValue));
    }

    // ---------------------------------------------------------------- settings

    protected final NumberSetting number(String name, double defaultValue, double min, double max, double step) {
        return addSetting(new NumberSetting(name, defaultValue, min, max, step));
    }

    protected final ModeSetting mode(String name, String defaultValue, String... modes) {
        return addSetting(new ModeSetting(name, defaultValue, modes));
    }

    protected final ColorSetting color(String name, int defaultArgb) {
        return addSetting(new ColorSetting(name, defaultArgb));
    }

    protected final <S extends Setting<?>> S addSetting(S setting) {
        for (Setting<?> existing : settings) {
            if (existing.getName().equalsIgnoreCase(setting.getName())) {
                throw new IllegalArgumentException(name + " already has a setting named " + setting.getName());
            }
        }
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public Setting<?> getSetting(String name) {
        for (Setting<?> setting : settings) {
            if (setting.getName().equalsIgnoreCase(name) || setting.getName().replace(" ", "").equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    // ---------------------------------------------------------------- info

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        try {
            if (enabled) {
                Client.events().subscribe(this);
                onEnable();
            } else {
                Client.events().unsubscribe(this);
                onDisable();
            }
        } catch (Throwable t) {
            Client.LOGGER.error("Error while {} {}", enabled ? "enabling" : "disabling", name, t);
        }
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    /**
     * Call from the constructor to turn the module on when it has no saved state yet.
     */
    protected final void enableByDefault() {
        this.enabledByDefault = true;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    final void captureDefaults() {
        defaultKey = key;
        hiddenByDefault = hidden;
    }

    public int getDefaultKey() {
        return defaultKey;
    }

    public boolean isHiddenByDefault() {
        return hiddenByDefault;
    }

    /**
     * Hidden modules are not shown in the HUD module list.
     */
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Extra info shown next to the name in the HUD module list (e.g. the current mode), or {@code null}.
     */
    public String getTag() {
        return null;
    }

    /**
     * Whether a world is loaded and the player exists.
     */
    protected final boolean inGame() {
        return mc.player != null && mc.level != null;
    }
}
