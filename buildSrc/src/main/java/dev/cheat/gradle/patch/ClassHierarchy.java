package dev.cheat.gradle.patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    private final Map<String, Info> cache = new HashMap<>();

    ClassHierarchy(Function<String, byte[]> gameClasses, List<Path> libraryJars) {
        this.gameClasses = gameClasses;
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
        if (bytes == null) return null;
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

    private static byte[] fromJdk(String name) {
        try (InputStream in = ClassLoader.getSystemResourceAsStream(name + ".class")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String commonSuperClass(String a, String b) {
        if (a.equals(b)) return a;
        Info ia = info(a);
        Info ib = info(b);
        if (ia == null || ib == null || ia.isInterface() || ib.isInterface()) return "java/lang/Object";
        java.util.Set<String> ancestors = new java.util.HashSet<>();
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
    }
}
