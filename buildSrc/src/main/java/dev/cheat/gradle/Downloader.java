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
        try {
            if (Files.isRegularFile(target) && (sha1 == null || sha1.equalsIgnoreCase(sha1(target)))) {
                return target;
            }
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".part");
            for (int attempt = 1; ; attempt++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofMinutes(5))
                            .header("User-Agent", "client-template-build")
                            .build();
                    HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() != 200) {
                        throw new IOException("HTTP " + response.statusCode() + " for " + url);
                    }
                    try (InputStream in = response.body()) {
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (sha1 != null && !sha1.equalsIgnoreCase(sha1(tmp))) {
                        throw new IOException("SHA-1 mismatch for " + url);
                    }
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    return target;
                } catch (IOException e) {
                    if (attempt >= 3) throw e;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to download " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while downloading " + url, e);
        }
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
