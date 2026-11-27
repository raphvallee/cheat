package dev.cheat.gradle.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Copies the generated version into the official launcher's {@code versions} folder and adds a launcher
 * installation for it. Existing profile files are backed up to {@code *.bak} before being changed.
 */
@DisableCachingByDefault(because = "Writes outside the build directory")
public abstract class InstallClientTask extends DefaultTask {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @InputDirectory
    public abstract DirectoryProperty getVersionDir();

    @Input
    public abstract Property<String> getVersionId();

    @Input
    public abstract Property<String> getProfileName();

    @Internal
    public abstract DirectoryProperty getMinecraftDir();

    @TaskAction
    public void run() throws IOException {
        String id = getVersionId().get();
        Path mcDir = getMinecraftDir().get().getAsFile().toPath();
        if (!Files.isDirectory(mcDir)) {
            throw new GradleException("Minecraft directory not found: " + mcDir + " (set minecraft_dir in gradle.properties)");
        }
        Path source = getVersionDir().get().getAsFile().toPath().resolve(id);
        Path target = mcDir.resolve("versions").resolve(id);
        Files.createDirectories(target);
        try (Stream<Path> files = Files.list(source)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                Files.copy(file, target.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        getLogger().lifecycle("Installed " + id + " to " + target);

        for (String name : new String[]{"launcher_profiles.json", "launcher_profiles_microsoft_store.json"}) {
            Path file = mcDir.resolve(name);
            if (Files.isRegularFile(file)) addProfile(file, id);
        }
    }

    private void addProfile(Path file, String id) throws IOException {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file)) {
            root = GSON.fromJson(reader, JsonObject.class);
        }
        if (root == null) root = new JsonObject();
        if (!root.has("profiles")) root.add("profiles", new JsonObject());
        JsonObject profiles = root.getAsJsonObject("profiles");
        JsonObject profile = profiles.has(id) ? profiles.getAsJsonObject(id) : new JsonObject();
        String now = Instant.now().toString();
        if (!profile.has("created")) profile.addProperty("created", now);
        profile.addProperty("lastUsed", now);
        profile.addProperty("icon", "Furnace");
        profile.addProperty("lastVersionId", id);
        profile.addProperty("name", getProfileName().get());
        profile.addProperty("type", "custom");
        profiles.add(id, profile);

        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(root, writer);
        }
        getLogger().lifecycle("Added launcher installation '" + getProfileName().get() + "' to " + file.getFileName());
    }
}
