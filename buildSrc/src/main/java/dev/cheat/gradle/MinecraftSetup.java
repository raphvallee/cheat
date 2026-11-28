package dev.cheat.gradle;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Resolves a vanilla Minecraft version from Mojang's servers: version JSON, client jar, libraries and assets.
 * Everything is cached under {@code <gradle user home>/caches/client-template/minecraft}.
 */
public final class MinecraftSetup {
    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final Gson GSON = new Gson();
    private static final ConcurrentHashMap<String, MinecraftSetup> CACHE = new ConcurrentHashMap<>();
    private static final Duration VERSION_JSON_MAX_AGE = Duration.ofDays(1);
    private static final Logger LOGGER = Logging.getLogger(MinecraftSetup.class);

    public final String version;
    public final Path root;
    public final Path versionJsonFile;
    public final JsonObject versionJson;
    public final Path clientJar;
    public final List<Path> libraries;

    private MinecraftSetup(String version, Path root) throws IOException {
        this.version = version;
        this.root = root;
        this.versionJsonFile = root.resolve("versions").resolve(version).resolve(version + ".json");

        refreshVersionJson();
        this.versionJson = read(versionJsonFile);

        JsonObject client = versionJson.getAsJsonObject("downloads").getAsJsonObject("client");
        this.clientJar = Downloader.download(client.get("url").getAsString(),
                root.resolve("versions").resolve(version).resolve(version + ".jar"), client.get("sha1").getAsString(),
                size(client));

        this.libraries = downloadLibraries();
    }

    private void refreshVersionJson() throws IOException {
        Files.deleteIfExists(root.resolve("version_manifest_v2.json.tmp"));
        boolean cached = Files.isRegularFile(versionJsonFile);
        if (cached && Files.getLastModifiedTime(versionJsonFile).toInstant()
                .isAfter(Instant.now().minus(VERSION_JSON_MAX_AGE))) {
            return;
        }
        JsonObject manifest;
        try {
            manifest = GSON.fromJson(new String(Downloader.fetch(MANIFEST), StandardCharsets.UTF_8), JsonObject.class);
        } catch (RuntimeException e) {
            if (!cached) throw e;
            LOGGER.warn("Could not check for an updated Minecraft {} version JSON, using the cached one: {}", version, e.getMessage());
            return;
        }
        JsonObject entry = null;
        for (JsonElement e : manifest.getAsJsonArray("versions")) {
            if (e.getAsJsonObject().get("id").getAsString().equals(version)) entry = e.getAsJsonObject();
        }
        if (entry == null) throw new IllegalArgumentException("Unknown Minecraft version: " + version);
        Downloader.download(entry.get("url").getAsString(), versionJsonFile, entry.get("sha1").getAsString());
        Files.setLastModifiedTime(versionJsonFile, FileTime.from(Instant.now()));
    }

    public static MinecraftSetup resolve(Path gradleUserHome, String version) {
        return CACHE.computeIfAbsent(version, v -> {
            try {
                return new MinecraftSetup(v, gradleUserHome.resolve("caches/client-template/minecraft"));
            } catch (IOException e) {
                throw new RuntimeException("Failed to set up Minecraft " + v, e);
            }
        });
    }

    private List<Path> downloadLibraries() {
        List<JsonObject> artifacts = new ArrayList<>();
        for (JsonElement e : versionJson.getAsJsonArray("libraries")) {
            JsonObject lib = e.getAsJsonObject();
            if (lib.has("rules") && !Rules.allowed(lib.getAsJsonArray("rules"))) continue;
            if (!lib.has("downloads") || !lib.getAsJsonObject("downloads").has("artifact")) continue;
            artifacts.add(lib.getAsJsonObject("downloads").getAsJsonObject("artifact"));
        }
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Path>> futures = new ArrayList<>();
        boolean done = false;
        try {
            for (JsonObject a : artifacts) {
                futures.add(pool.submit(() -> Downloader.download(a.get("url").getAsString(),
                        root.resolve("libraries").resolve(a.get("path").getAsString()), a.get("sha1").getAsString(),
                        size(a))));
            }
            List<Path> result = new ArrayList<>();
            for (Future<Path> f : futures) result.add(f.get());
            done = true;
            return List.copyOf(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download libraries", e);
        } finally {
            shutdown(pool, futures, done);
        }
    }

    /**
     * Returns an assets directory containing this version's asset index and objects. Reuses the official launcher's
     * {@code .minecraft/assets} when it already has all of them, otherwise downloads what is missing into the cache.
     */
    public Path assetsDir(Path minecraftDir) throws IOException {
        JsonObject index = versionJson.getAsJsonObject("assetIndex");
        String id = index.get("id").getAsString();
        Path launcherAssets = minecraftDir.resolve("assets");
        Path launcherIndex = launcherAssets.resolve("indexes").resolve(id + ".json");
        if (Files.isRegularFile(launcherIndex)) {
            try {
                if (hasAllObjects(launcherAssets, read(launcherIndex).getAsJsonObject("objects"))) return launcherAssets;
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Ignoring unreadable launcher asset index {}: {}", launcherIndex, e.getMessage());
            }
        }
        Path assets = root.resolve("assets");
        Path indexFile = Downloader.download(index.get("url").getAsString(),
                assets.resolve("indexes").resolve(id + ".json"), index.get("sha1").getAsString());
        JsonObject objects = read(indexFile).getAsJsonObject("objects");
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<Path>> futures = new ArrayList<>();
        boolean done = false;
        try {
            Set<String> seen = new HashSet<>();
            for (String key : objects.keySet()) {
                JsonObject object = objects.getAsJsonObject(key);
                String hash = object.get("hash").getAsString();
                if (!seen.add(hash)) continue;
                String sub = hash.substring(0, 2) + "/" + hash;
                long size = size(object);
                futures.add(pool.submit(() -> Downloader.download("https://resources.download.minecraft.net/" + sub,
                        assets.resolve("objects").resolve(sub), hash, size)));
            }
            for (Future<Path> f : futures) f.get();
            done = true;
        } catch (Exception e) {
            throw new IOException("Failed to download assets", e);
        } finally {
            shutdown(pool, futures, done);
        }
        return assets;
    }

    /** After a failure, stops the queued and running downloads instead of letting them finish. */
    private static void shutdown(ExecutorService pool, List<? extends Future<?>> futures, boolean done) {
        if (done) {
            pool.shutdown();
            return;
        }
        for (Future<?> f : futures) f.cancel(true);
        pool.shutdownNow();
    }

    private static boolean hasAllObjects(Path assets, JsonObject objects) throws IOException {
        for (String key : objects.keySet()) {
            JsonObject object = objects.getAsJsonObject(key);
            String hash = object.get("hash").getAsString();
            Path file = assets.resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
            if (!Files.isRegularFile(file) || Files.size(file) != size(object)) return false;
        }
        return true;
    }

    private static long size(JsonObject download) {
        return download.has("size") ? download.get("size").getAsLong() : -1;
    }

    public String assetIndexId() {
        return versionJson.getAsJsonObject("assetIndex").get("id").getAsString();
    }

    public String mainClass() {
        return versionJson.get("mainClass").getAsString();
    }

    /** Fixed JVM arguments from the version JSON that apply to this OS (placeholders not substituted). */
    public List<String> jvmArguments() {
        List<String> out = new ArrayList<>();
        JsonArray jvm = versionJson.getAsJsonObject("arguments").getAsJsonArray("jvm");
        for (JsonElement e : jvm) {
            if (e.isJsonPrimitive()) {
                out.add(e.getAsString());
            } else {
                JsonObject o = e.getAsJsonObject();
                if (!Rules.allowed(o.getAsJsonArray("rules"))) continue;
                JsonElement value = o.get("value");
                if (value.isJsonArray()) value.getAsJsonArray().forEach(v -> out.add(v.getAsString()));
                else out.add(value.getAsString());
            }
        }
        return out;
    }

    private static JsonObject read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, JsonObject.class);
        }
    }

    /** Evaluates Mojang's library/argument rules for the current machine. Feature rules never match. */
    public static final class Rules {
        private Rules() {}

        public static boolean allowed(JsonArray rules) {
            boolean allowed = false;
            for (JsonElement e : rules) {
                JsonObject rule = e.getAsJsonObject();
                if (rule.has("features")) continue;
                if (rule.has("os") && !osMatches(rule.getAsJsonObject("os"))) continue;
                allowed = rule.get("action").getAsString().equals("allow");
            }
            return allowed;
        }

        private static boolean osMatches(JsonObject os) {
            if (os.has("name") && !os.get("name").getAsString().equals(osName())) return false;
            if (os.has("arch")) {
                String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
                boolean x86 = arch.equals("x86") || arch.equals("i386") || arch.equals("i686");
                if (os.get("arch").getAsString().equals("x86") != x86) return false;
            }
            return true;
        }

        public static String osName() {
            String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if (name.contains("win")) return "windows";
            if (name.contains("mac")) return "osx";
            return "linux";
        }
    }
}
