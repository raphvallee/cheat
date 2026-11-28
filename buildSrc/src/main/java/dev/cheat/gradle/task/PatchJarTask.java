package dev.cheat.gradle.task;

import dev.cheat.gradle.patch.GamePatches;
import dev.cheat.gradle.patch.JarPatcher;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Path;

/** Produces the client jar: vanilla classes with {@link GamePatches} applied, plus the client's own files. */
public abstract class PatchJarTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVanillaJar();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getClientFiles();

    @Classpath
    public abstract ConfigurableFileCollection getLibraries();

    @Internal
    public abstract DirectoryProperty getJdkHome();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void run() {
        try {
            JarPatcher.patch(
                    getVanillaJar().get().getAsFile().toPath(),
                    getClientFiles().getFiles().stream().map(File::toPath).toList(),
                    getLibraries().getFiles().stream().map(File::toPath).toList(),
                    getJdkHome().isPresent() ? getJdkHome().get().getAsFile().toPath() : null,
                    getOutputJar().get().getAsFile().toPath(),
                    GamePatches.HOOKS,
                    GamePatches.all(),
                    msg -> getLogger().lifecycle(msg));
        } catch (Exception e) {
            throw new GradleException(e.getMessage(), e);
        }
    }

    static Path path(RegularFileProperty property) {
        return property.get().getAsFile().toPath();
    }
}
