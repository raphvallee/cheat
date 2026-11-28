package dev.cheat.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/** Small cached HTTP downloader with SHA-1 verification. */
public final class Downloader {
    private static final int ATTEMPTS = 3;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private Downloader() {}

    /**
     * Downloads {@code url} to {@code target} unless it already exists with the expected hash.
     *
     * @param sha1 expected SHA-1 (hex) or {@code null} to skip verification
     */
    public static Path download(String url, Path target, String sha1) {
        return download(url, target, sha1, -1);
    }

    public static Path download(String url, Path target, String sha1, long size) {
        try {
            if (isPresent(target, sha1, size)) return target;
            Files.createDirectories(target.getParent());
            IOException failure = null;
            for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
                Path tmp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
                try {
                    fetchTo(url, tmp);
                    if (sha1 != null && !sha1.equalsIgnoreCase(sha1(tmp))) {
                        throw new IOException("SHA-1 mismatch for " + url);
                    }
                    try {
                        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException e) {
                        if (!isPresent(target, sha1, -1)) throw e;
                    }
                    return target;
                } catch (IOException e) {
                    if (failure == null) failure = e;
                    else failure.addSuppressed(e);
                } finally {
                    Files.deleteIfExists(tmp);
                }
            }
            throw failure;
        } catch (IOException e) {
            throw new RuntimeException("Failed to download " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while downloading " + url, e);
        }
    }

    public static byte[] fetch(String url) {
        IOException failure = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try (InputStream in = open(url)) {
                return in.readAllBytes();
            } catch (IOException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while downloading " + url, e);
            }
        }
        throw new RuntimeException("Failed to download " + url, failure);
    }

    private static boolean isPresent(Path target, String sha1, long size) throws IOException {
        if (!Files.isRegularFile(target)) return false;
        if (size >= 0) return Files.size(target) == size;
        return sha1 == null || sha1.equalsIgnoreCase(sha1(target));
    }

    private static void fetchTo(String url, Path file) throws IOException, InterruptedException {
        try (InputStream in = open(url)) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static InputStream open(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "client-template-build")
                .build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    public static String sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[1 << 16];
                int read;
                while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
