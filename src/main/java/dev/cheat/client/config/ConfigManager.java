package dev.cheat.client.config;

import com.google.gson.*;
import dev.cheat.client.Client;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.ModuleManager;
import dev.cheat.client.module.impl.client.ClickGuiModule;
import dev.cheat.client.module.setting.Setting;
import dev.cheat.client.util.KeyUtil;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Saves and loads module state (enabled, bind, hidden, settings) and GUI layout as JSON.
 *
 * <p>The active config is {@code <gameDir>/<client>/config.json}; it is loaded at startup and saved when the
 * ClickGUI closes, when the game exits and when it crashes. Named configs live in {@code <gameDir>/<client>/configs/}.
 * Loading a config resets everything it does not contain to the defaults. An unreadable {@code config.json} is
 * renamed to {@code config.broken-<date>.json} instead of being overwritten.</p>
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Pattern NAME = Pattern.compile("[a-z0-9_-][a-z0-9_.-]{0,63}");
    private static final Set<String> RESERVED = Set.of("con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");
    private static final DateTimeFormatter BROKEN_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final Path directory;
    private final ModuleManager modules;
    /**
     * What this manager last wrote to each file, so an unchanged config is not rewritten.
     */
    private final Map<Path, Written> written = new ConcurrentHashMap<>();
    public ConfigManager(Path directory, ModuleManager modules) {
        this.directory = directory;
        this.modules = modules;
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static boolean bool(JsonObject parent, String key, boolean fallback, Module module) {
        JsonElement element = parent.get(key);
        if (element == null) return fallback;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) return element.getAsBoolean();
        Client.LOGGER.warn("Ignoring invalid '{}' for {}", key, module.getName());
        return fallback;
    }

    private static JsonObject read(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) throw new JsonParseException("empty file");
            return json;
        } catch (IOException | RuntimeException e) {
            Client.LOGGER.error("Could not read config {}", file, e);
            return null;
        }
    }

    // ---------------------------------------------------------------- active config

    private static void moveAside(Path file) {
        String name = file.getFileName().toString();
        String base = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        Path target = file.resolveSibling(base + ".broken-" + LocalDateTime.now().format(BROKEN_SUFFIX) + ".json");
        try {
            Files.move(file, target);
            Client.LOGGER.warn("Moved the unreadable config to {}; defaults are used instead", target);
        } catch (IOException e) {
            Client.LOGGER.error("Could not move the unreadable config {} aside", file, e);
        }
    }

    private static FileTime modifiedTime(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- named configs

    public Path getDirectory() {
        return directory;
    }

    public void load() {
        Path file = directory.resolve("config.json");
        JsonObject json = null;
        if (Files.isRegularFile(file)) {
            json = read(file);
            if (json == null) moveAside(file);
        }
        apply(json == null ? new JsonObject() : json);
    }

    public boolean save() {
        return write(directory.resolve("config.json"), serialize());
    }

    public LoadResult load(String name) {
        Path file = named(name);
        if (!Files.isRegularFile(file)) return LoadResult.NOT_FOUND;
        JsonObject json = read(file);
        if (json == null) return LoadResult.INVALID;
        apply(json);
        return LoadResult.LOADED;
    }

    // ---------------------------------------------------------------- (de)serialization

    public boolean save(String name) {
        return write(named(name), serialize());
    }

    public List<String> list() {
        Path dir = directory.resolve("configs");
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".json") && !n.contains(".broken-"))
                        .map(n -> n.substring(0, n.length() - 5))
                        .sorted()
                        .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private Path named(String name) {
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (!NAME.matcher(n).matches()) {
            throw new IllegalArgumentException("Invalid config name '" + name
                    + "': use up to 64 letters, digits, '_', '-' or '.'.");
        }
        String base = n.contains(".") ? n.substring(0, n.indexOf('.')) : n;
        if (RESERVED.contains(base) || n.endsWith(".")) {
            throw new IllegalArgumentException("Invalid config name '" + name + "'.");
        }
        return directory.resolve("configs").resolve(n + ".json");
    }

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
        JsonObject modulesJson = object(root, "modules");
        for (Module module : modules.getModules()) {
            JsonObject m = object(modulesJson, module.getName());
            JsonObject settings = object(m, "settings");
            for (Setting<?> setting : module.getSettings()) {
                setting.reset();
                JsonElement value = settings.get(setting.getName());
                if (value == null) continue;
                try {
                    setting.fromJson(value);
                } catch (RuntimeException e) {
                    Client.LOGGER.warn("Ignoring invalid value for {} {}", module.getName(), setting.getName(), e);
                    setting.reset();
                }
            }
            int key = module.getDefaultKey();
            try {
                if (m.has("key")) {
                    int saved = m.get("key").getAsInt();
                    if (KeyUtil.isValid(saved)) key = saved;
                    else Client.LOGGER.warn("Ignoring invalid key {} for {}", saved, module.getName());
                }
            } catch (RuntimeException e) {
                Client.LOGGER.warn("Ignoring invalid key for {}", module.getName(), e);
            }
            module.setKey(key);
            module.setHidden(bool(m, "hidden", module.isHiddenByDefault(), module));
            module.setEnabled(bool(m, "enabled", m.isEmpty() && module.isEnabledByDefault(), module));
        }
        ClickGuiModule clickGui = modules.getModule(ClickGuiModule.class);
        JsonObject panels = object(root, "clickgui");
        if (clickGui != null && !panels.isEmpty()) clickGui.panelsFromJson(panels);
    }

    private boolean write(Path file, JsonObject json) {
        String text = GSON.toJson(json);
        Written last = written.get(file);
        if (last != null && last.json.equals(text) && last.modified.equals(modifiedTime(file))) return true;
        Path tmp = null;
        try {
            Files.createDirectories(file.getParent());
            tmp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            Files.writeString(tmp, text);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            FileTime modified = modifiedTime(file);
            if (modified != null) written.put(file, new Written(text, modified));
            else written.remove(file);
            return true;
        } catch (IOException e) {
            written.remove(file);
            Client.LOGGER.error("Could not save config {}", file, e);
            return false;
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public enum LoadResult {LOADED, NOT_FOUND, INVALID}

    private record Written(String json, FileTime modified) {
    }
}
