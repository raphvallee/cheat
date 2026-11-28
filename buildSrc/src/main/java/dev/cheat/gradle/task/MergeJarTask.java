package dev.cheat.gradle.task;

import dev.cheat.gradle.patch.JarPatcher;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/** Produces the runnable client jar: the patched vanilla jar, the client's own files and a manifest. */
public abstract class MergeJarTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getPatchedJar();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getClientFiles();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void run() {
        try {
            JarPatcher.merge(
                    getPatchedJar().get().getAsFile().toPath(),
                    getClientFiles().getFiles().stream().map(File::toPath).toList(),
                    getOutputJar().get().getAsFile().toPath(),
                    msg -> getLogger().lifecycle(msg));
        } catch (Exception e) {
            throw new GradleException(e.getMessage(), e);
        }
    }
}
