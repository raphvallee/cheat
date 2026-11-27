package dev.cheat.client.module;

import dev.cheat.client.event.EventBus;
import dev.cheat.client.event.Subscribe;
import dev.cheat.client.event.events.KeyInputEvent;
import dev.cheat.client.module.impl.client.ClickGuiModule;
import dev.cheat.client.module.impl.client.HudModule;
import dev.cheat.client.module.impl.combat.Velocity;
import dev.cheat.client.module.impl.movement.Sprint;
import dev.cheat.client.module.impl.player.NoFall;
import dev.cheat.client.module.impl.render.Esp;
import dev.cheat.client.util.KeyUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns every module and handles their key binds. */
public final class ModuleManager {
    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager(EventBus eventBus) {
        eventBus.subscribe(this);
    }

    /** Add new modules here. */
    public void registerDefaults() {
        register(
                // Combat
                new Velocity(),
                // Movement
                new Sprint(),
                // Player
                new NoFall(),
                // Render
                new Esp(),
                // Client
                new HudModule(),
                new ClickGuiModule()
        );
        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
    }

    public void register(Module... toAdd) {
        for (Module module : toAdd) {
            if (getModule(module.getName()) != null) {
                throw new IllegalArgumentException("Duplicate module name " + module.getName());
            }
            byClass.put(module.getClass(), module);
            modules.add(module);
        }
    }

    @Subscribe
    private void onKey(KeyInputEvent event) {
        if (!event.isPress() || event.isInScreen() || event.getKey() == KeyUtil.NONE) return;
        for (Module module : modules) {
            if (module.getKey() == event.getKey()) module.toggle();
        }
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getModules(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> type) {
        return (T) byClass.get(type);
    }

    /** Finds a module by name, ignoring case and spaces. */
    public Module getModule(String name) {
        String wanted = name.replace(" ", "");
        for (Module module : modules) {
            if (module.getName().replace(" ", "").equalsIgnoreCase(wanted)) return module;
        }
        return null;
    }
}
