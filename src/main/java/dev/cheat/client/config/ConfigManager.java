package dev.cheat.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.cheat.client.Client;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.ModuleManager;
import dev.cheat.client.module.impl.client.ClickGuiModule;
import dev.cheat.client.module.setting.Setting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * Saves and loads module state (enabled, bind, hidden, settings) and GUI layout as JSON.
 *
 * <p>The active config is {@code <gameDir>/<client>/config.json}; it is loaded at startup and saved when the
 * ClickGUI closes and when the game exits. Named configs live in {@code <gameDir>/<client>/configs/}.</p>
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path directory;
    private final ModuleManager modules;

    public ConfigManager(Path directory, ModuleManager modules) {
        this.directory = directory;
        this.modules = modules;
    }

    public Path getDirectory() {
        return directory;
    }

    // ---------------------------------------------------------------- active config

    public void load() {
        Path file = directory.resolve("config.json");
        JsonObject json = Files.isRegularFile(file) ? read(file) : null;
        apply(json == null ? new JsonObject() : json);
    }

    public void save() {
        write(directory.resolve("config.json"), serialize());
    }

    // ---------------------------------------------------------------- named configs

    public boolean load(String name) {
        Path file = named(name);
        if (!Files.isRegularFile(file)) return false;
        JsonObject json = read(file);
        if (json == null) return false;
        apply(json);
        return true;
    }

    public void save(String name) {
        write(named(name), serialize());
    }

    public List<String> list() {
        Path dir = directory.resolve("configs");
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private Path named(String name) {
        String safe = name.replaceAll("[^A-Za-z0-9_.-]", "_");
        return directory.resolve("configs").resolve(safe + ".json");
    }

    // ---------------------------------------------------------------- (de)serialization

    private JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject modulesJson = new JsonObject();
        for (Module module : modules.getModules()) {
            JsonObject m = new JsonObject();
            m.addProperty("enabled", module.isEnabled());
            m.addProperty("key", module.getKey());
            m.addProperty("hidden", module.isHidden());
            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.getSettings()) settings.add(setting.getName(), setting.toJson());
            m.add("settings", settings);
            modulesJson.add(module.getName(), m);
        }
        root.add("modules", modulesJson);
        ClickGuiModule clickGui = modules.getModule(ClickGuiModule.class);
        if (clickGui != null) root.add("clickgui", clickGui.panelsToJson());
        return root;
    }

    private void apply(JsonObject root) {
        JsonObject modulesJson = root.has("modules") && root.get("modules").isJsonObject()
                ? root.getAsJsonObject("modules") : new JsonObject();
        for (Module module : modules.getModules()) {
            JsonElement element = modulesJson.get(module.getName());
            if (element == null || !element.isJsonObject()) {
                module.setEnabled(module.isEnabledByDefault());
                continue;
            }
            JsonObject m = element.getAsJsonObject();
            try {
                if (m.has("settings") && m.get("settings").isJsonObject()) {
                    JsonObject settings = m.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        JsonElement value = settings.get(setting.getName());
                        if (value != null) setting.fromJson(value);
                    }
                }
                if (m.has("key")) module.setKey(m.get("key").getAsInt());
                if (m.has("hidden")) module.setHidden(m.get("hidden").getAsBoolean());
                module.setEnabled(m.has("enabled") && m.get("enabled").getAsBoolean());
            } catch (RuntimeException e) {
                Client.LOGGER.warn("Ignoring invalid config entry for {}", module.getName(), e);
            }
        }
        ClickGuiModule clickGui = modules.getModule(ClickGuiModule.class);
        if (clickGui != null && root.has("clickgui") && root.get("clickgui").isJsonObject()) {
            clickGui.panelsFromJson(root.getAsJsonObject("clickgui"));
        }
    }

    private static JsonObject read(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (IOException | RuntimeException e) {
            Client.LOGGER.error("Could not read config {}", file, e);
            return null;
        }
    }

    private static void write(Path file, JsonObject json) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(json, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Client.LOGGER.error("Could not save config {}", file, e);
        }
    }
}
