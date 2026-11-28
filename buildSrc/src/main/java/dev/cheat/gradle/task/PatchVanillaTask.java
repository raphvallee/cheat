package dev.cheat.gradle.task;

import dev.cheat.gradle.patch.GamePatches;
import dev.cheat.gradle.patch.JarPatcher;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * Applies {@link GamePatches} to the vanilla jar and removes Mojang's signature. It reads only the compiled hooks
 * class from the client, so other client changes leave this task up to date.
 */
public abstract class PatchVanillaTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVanillaJar();

    /** The compiled hooks class. A file collection so that a missing file gets the patcher's own error. */
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getHooksClass();

    @Classpath
    public abstract ConfigurableFileCollection getLibraries();

    @Internal
    public abstract DirectoryProperty getJdkHome();

    /** Version of the JDK at {@link #getJdkHome()}, whose classes are used for frame computation. */
    @Input
    public abstract Property<String> getJdkVersion();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void run() {
        try {
            JarPatcher.patchVanilla(
                    getVanillaJar().get().getAsFile().toPath(),
                    getHooksClass().getFiles().stream().map(File::toPath).findFirst().orElse(null),
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
}
