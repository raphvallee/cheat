package dev.cheat.gradle.patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Answers super class / interface questions for {@code ClassWriter.COMPUTE_FRAMES} by reading class headers
 * from the game jar, its libraries and the JDK, without ever loading game classes.
 */
final class ClassHierarchy implements AutoCloseable {
    record Info(String superName, boolean isInterface) {}

    private final Function<String, byte[]> gameClasses;
    private final List<ZipFile> libraries;
    private final FileSystem jdk;
    private final Map<String, Info> cache = new HashMap<>();
    private final Set<String> missing = new TreeSet<>();

    ClassHierarchy(Function<String, byte[]> gameClasses, List<Path> libraryJars, Path jdkHome) throws IOException {
        this.gameClasses = gameClasses;
        this.jdk = jdkHome == null ? null
                : FileSystems.newFileSystem(URI.create("jrt:/"), Map.of("java.home", jdkHome.toString()));
        this.libraries = libraryJars.stream().map(p -> {
            try {
                return new ZipFile(p.toFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).toList();
    }

    Info info(String name) {
        return cache.computeIfAbsent(name, this::load);
    }

    private Info load(String name) {
        byte[] bytes = gameClasses.apply(name);
        if (bytes == null) bytes = fromLibraries(name);
        if (bytes == null) bytes = fromJdk(name);
        if (bytes == null) {
            missing.add(name.replace('/', '.'));
            return null;
        }
        ClassReader reader = new ClassReader(bytes);
        return new Info(reader.getSuperName(), (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0);
    }

    private byte[] fromLibraries(String name) {
        for (ZipFile zip : libraries) {
            ZipEntry entry = zip.getEntry(name + ".class");
            if (entry != null) {
                try (InputStream in = zip.getInputStream(entry)) {
                    return in.readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return null;
    }

    private byte[] fromJdk(String name) {
        try {
            if (jdk == null) {
                try (InputStream in = ClassLoader.getSystemResourceAsStream(name + ".class")) {
                    return in == null ? null : in.readAllBytes();
                }
            }
            int slash = name.lastIndexOf('/');
            if (slash < 0) return null;
            Path packageDir = jdk.getPath("/packages", name.substring(0, slash).replace('/', '.'));
            if (!Files.isDirectory(packageDir)) return null;
            try (Stream<Path> modules = Files.list(packageDir)) {
                for (Path module : (Iterable<Path>) modules::iterator) {
                    Path file = jdk.getPath("/modules", module.getFileName().toString(), name + ".class");
                    if (Files.isRegularFile(file)) return Files.readAllBytes(file);
                }
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Set<String> missing() {
        return missing;
    }

    String commonSuperClass(String a, String b) {
        if (a.equals(b)) return a;
        Info ia = info(a);
        Info ib = info(b);
        if (ia == null || ib == null || ia.isInterface() || ib.isInterface()) return "java/lang/Object";
        Set<String> ancestors = new HashSet<>();
        for (String c = a; c != null; c = superOf(c)) ancestors.add(c);
        for (String c = b; c != null; c = superOf(c)) {
            if (ancestors.contains(c)) return c;
        }
        return "java/lang/Object";
    }

    private String superOf(String name) {
        Info info = info(name);
        return info == null ? null : info.superName();
    }

    @Override
    public void close() throws IOException {
        for (ZipFile zip : libraries) zip.close();
        if (jdk != null) jdk.close();
    }
}
