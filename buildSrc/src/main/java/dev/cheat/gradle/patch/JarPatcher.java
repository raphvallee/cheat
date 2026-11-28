package dev.cheat.gradle.patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Builds the client jar: vanilla jar + bytecode hooks + client classes/resources, minus Mojang's signature.
 */
public final class JarPatcher {
    private JarPatcher() {}

    /**
     * @param vanillaJar  the untouched Mojang client jar
     * @param clientRoots compiled client classes and resources (directories)
     * @param libraries   game libraries, used to resolve the class hierarchy for frame computation
     * @param jdkHome     JDK the game runs on, used to resolve JDK classes, or {@code null} for the build's JDK
     * @param output      jar to write
     * @param hooksClass  internal name of the hooks class, e.g. {@code dev/cheat/client/hook/Hooks}
     * @param injections  every injection to apply
     * @param log         progress output
     */
    public static void patch(Path vanillaJar, List<Path> clientRoots, List<Path> libraries, Path jdkHome, Path output,
                             String hooksClass, List<Injection> injections, Consumer<String> log) throws IOException {
        injections.forEach(Injection::validate);

        Map<String, byte[]> vanilla = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(vanillaJar.toFile())) {
            for (ZipEntry entry : java.util.Collections.list(zip.entries())) {
                if (entry.isDirectory() || isSignatureOrManifest(entry.getName())) continue;
                try (InputStream in = zip.getInputStream(entry)) {
                    vanilla.put(entry.getName(), in.readAllBytes());
                }
            }
        }

        Map<String, byte[]> client = new LinkedHashMap<>();
        for (Path root : clientRoots) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                    String name = root.relativize(file).toString().replace('\\', '/');
                    if (isSignatureOrManifest(name)) {
                        throw new IllegalStateException("Client file " + name + " is not allowed: the patched jar"
                                + " writes its own manifest and must not be signed");
                    }
                    if (vanilla.containsKey(name)) {
                        throw new IllegalStateException("Client file " + name + " would overwrite a vanilla file");
                    }
                    client.put(name, Files.readAllBytes(file));
                }
            }
        }

        byte[] hooksBytes = client.get(hooksClass + ".class");
        if (hooksBytes == null) throw new IllegalStateException("Hooks class " + hooksClass + " was not compiled");
        Set<String> hookMethods = staticMethods(hooksBytes);

        Map<String, List<Injection>> byOwner = new LinkedHashMap<>();
        for (Injection injection : injections) byOwner.computeIfAbsent(injection.owner, k -> new ArrayList<>()).add(injection);

        List<String> errors = new ArrayList<>();
        try (ClassHierarchy hierarchy = new ClassHierarchy(name -> {
            byte[] b = vanilla.get(name + ".class");
            return b != null ? b : client.get(name + ".class");
        }, libraries, jdkHome)) {
            for (Map.Entry<String, List<Injection>> e : byOwner.entrySet()) {
                String owner = e.getKey();
                byte[] original = vanilla.get(owner + ".class");
                if (original == null) {
                    errors.add("Target class not found: " + owner);
                    continue;
                }
                ClassNode node = new ClassNode();
                new ClassReader(original).accept(node, ClassReader.SKIP_FRAMES);
                for (Injection injection : e.getValue()) {
                    MethodNode method = node.methods.stream()
                            .filter(m -> m.name.equals(injection.name) && m.desc.equals(injection.desc))
                            .findFirst().orElse(null);
                    if (method == null) {
                        errors.add("Target method not found: " + injection.target());
                        continue;
                    }
                    boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
                    String hookDesc = injection.hookDesc(isStatic);
                    if (!hookMethods.contains(injection.hook + hookDesc)) {
                        errors.add("Missing hook for " + injection + "\n    expected: public static "
                                + javaSignature(injection.hook, hookDesc) + " in " + hooksClass.replace('/', '.'));
                        continue;
                    }
                    String unsupported = injection.unsupportedReason(method);
                    if (unsupported != null) {
                        errors.add("Cannot apply " + injection + ": " + unsupported);
                        continue;
                    }
                    int sites = injection.apply(method, hooksClass);
                    if (sites == 0) {
                        errors.add("Injection point not found: " + injection);
                        continue;
                    }
                    log.accept("  patched " + injection + (sites > 1 ? " [" + sites + " sites]" : ""));
                }
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                    @Override
                    protected String getCommonSuperClass(String a, String b) {
                        return hierarchy.commonSuperClass(a, b);
                    }
                };
                node.accept(writer);
                vanilla.put(owner + ".class", writer.toByteArray());
            }
            if (!hierarchy.missing().isEmpty()) {
                log.accept("Warning: classes not found while computing stack frames (treated as subclasses of"
                        + " Object, which can cause a VerifyError at startup): " + String.join(", ", hierarchy.missing()));
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Patching failed:\n  " + String.join("\n  ", errors));
        }

        Files.createDirectories(output.getParent());
        Path tmp = output.resolveSibling(output.getFileName() + ".tmp");
        try (OutputStream fos = Files.newOutputStream(tmp); ZipOutputStream out = new ZipOutputStream(fos)) {
            Set<String> written = new HashSet<>();
            written.add("META-INF/MANIFEST.MF");
            out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            out.write("Manifest-Version: 1.0\r\nMain-Class: net.minecraft.client.main.Main\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            for (Map<String, byte[]> map : List.of(vanilla, client)) {
                for (Map.Entry<String, byte[]> entry : map.entrySet()) {
                    if (!written.add(entry.getKey())) continue;
                    out.putNextEntry(new ZipEntry(entry.getKey()));
                    out.write(entry.getValue());
                    out.closeEntry();
                }
            }
        }
        try {
            Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING);
        } catch (FileSystemException e) {
            Files.deleteIfExists(tmp);
            throw new IOException("Cannot replace " + output + ". Is the game still running? Close it and build again.", e);
        }
        log.accept("Wrote " + output + " (" + injections.size() + " injections, " + client.size() + " client files)");
    }

    private static boolean isSignatureOrManifest(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) return false;
        return upper.equals("META-INF/MANIFEST.MF") || upper.endsWith(".SF") || upper.endsWith(".RSA")
                || upper.endsWith(".DSA") || upper.endsWith(".EC");
    }

    private static Set<String> staticMethods(byte[] classBytes) {
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, ClassReader.SKIP_CODE);
        Set<String> out = new HashSet<>();
        for (MethodNode m : node.methods) {
            if ((m.access & Opcodes.ACC_STATIC) != 0 && (m.access & Opcodes.ACC_PUBLIC) != 0) out.add(m.name + m.desc);
        }
        return out;
    }

    private static String javaSignature(String name, String desc) {
        StringBuilder sb = new StringBuilder(Type.getReturnType(desc).getClassName()).append(' ').append(name).append('(');
        Type[] args = Type.getArgumentTypes(desc);
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(args[i].getClassName().replace('$', '.')).append(" arg").append(i);
        }
        return sb.append(')').toString();
    }
}
