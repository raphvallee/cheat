package dev.cheat.gradle.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes a standalone launcher version folder ({@code <id>/<id>.json} + {@code <id>/<id>.jar}).
 * The JSON is the vanilla one with a new id and without the client download entry, so the launcher
 * uses our jar instead of re-downloading the vanilla one.
 */
public abstract class CreateVersionTask extends DefaultTask {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Path path(RegularFileProperty property) {
        return property.get().getAsFile().toPath();
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVanillaVersionJson();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getClientJar();

    @Input
    public abstract Property<String> getVersionId();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void run() throws IOException {
        String id = getVersionId().get();
        Path dir = getOutputDir().get().getAsFile().toPath().resolve(id);
        Files.createDirectories(dir);

        JsonObject json;
        try (Reader reader = Files.newBufferedReader(path(getVanillaVersionJson()))) {
            json = GSON.fromJson(reader, JsonObject.class);
        }
        json.addProperty("id", id);
        json.getAsJsonObject("downloads").remove("client");
        try (Writer writer = Files.newBufferedWriter(dir.resolve(id + ".json"))) {
            GSON.toJson(json, writer);
        }
        Files.copy(path(getClientJar()), dir.resolve(id + ".jar"), StandardCopyOption.REPLACE_EXISTING);
        getLogger().lifecycle("Created launcher version " + dir);
    }
}
