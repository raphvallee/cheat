package dev.cheat.gradle;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Logs in with a Microsoft account (device code flow) and returns a Minecraft session.
 *
 * <p>Chain: Microsoft (live.com) -> Xbox Live -> XSTS -> Minecraft services. The refresh token and the
 * last Minecraft token are cached in {@code sessionFile}, so the browser login is only needed once.
 */
public final class MicrosoftAuth {
    /** Public client id of the Minecraft launcher, accepted by the Xbox Live RPS ticket exchange. */
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final Duration TOKEN_MARGIN = Duration.ofMinutes(5);

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private MicrosoftAuth() {}

    public record Session(String username, String uuid, String accessToken, String xuid) {}

    /**
     * Returns a valid session, reusing or refreshing the cached one when possible.
     *
     * @param log receives the device code instructions and progress messages
     */
    public static Session login(Path sessionFile, Consumer<String> log) {
        try {
            JsonObject cache = readCache(sessionFile);
            if (cache != null && cache.has("expiresAt")
                    && Instant.ofEpochSecond(cache.get("expiresAt").getAsLong()).isAfter(Instant.now().plus(TOKEN_MARGIN))) {
                return toSession(cache);
            }

            JsonObject msa = null;
            if (cache != null && cache.has("refreshToken")) {
                try {
                    msa = refresh(cache.get("refreshToken").getAsString());
                } catch (AuthException e) {
                    log.accept("Cached Microsoft login is no longer valid (" + e.getMessage() + "), logging in again.");
                }
            }
            if (msa == null) msa = deviceCode(log);

            JsonObject result = minecraftLogin(msa.get("access_token").getAsString());
            result.addProperty("refreshToken", msa.get("refresh_token").getAsString());
            writeCache(sessionFile, result);
            log.accept("Logged in as " + result.get("username").getAsString() + ".");
            return toSession(result);
        } catch (IOException e) {
            throw new IllegalStateException("Microsoft login failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Microsoft login interrupted", e);
        }
    }

    private static Session toSession(JsonObject cache) {
        return new Session(
                cache.get("username").getAsString(),
                cache.get("uuid").getAsString(),
                cache.get("accessToken").getAsString(),
                cache.has("xuid") ? cache.get("xuid").getAsString() : "0");
    }

    // ---- Microsoft account ----

    private static JsonObject deviceCode(Consumer<String> log) throws IOException, InterruptedException {
        JsonObject code = postForm("https://login.live.com/oauth20_connect.srf", Map.of(
                "client_id", CLIENT_ID,
                "scope", SCOPE,
                "response_type", "device_code"));
        String uri = code.get("verification_uri").getAsString();
        String userCode = code.get("user_code").getAsString();
        log.accept("");
        log.accept("To sign in, open " + uri + " and enter the code " + userCode);
        log.accept("");

        long interval = code.has("interval") ? code.get("interval").getAsLong() : 5;
        Instant deadline = Instant.now().plusSeconds(code.get("expires_in").getAsLong());
        while (Instant.now().isBefore(deadline)) {
            Thread.sleep(Duration.ofSeconds(interval));
            JsonObject token = postFormRaw("https://login.live.com/oauth20_token.srf", Map.of(
                    "client_id", CLIENT_ID,
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                    "device_code", code.get("device_code").getAsString()));
            if (token.has("access_token")) return token;
            String error = token.has("error") ? token.get("error").getAsString() : "unknown_error";
            switch (error) {
                case "authorization_pending" -> {}
                case "slow_down" -> interval += 5;
                default -> throw new AuthException("device code login failed: " + error);
            }
        }
        throw new AuthException("the device code expired before the login was completed");
    }

    private static JsonObject refresh(String refreshToken) throws IOException, InterruptedException {
        JsonObject token = postFormRaw("https://login.live.com/oauth20_token.srf", Map.of(
                "client_id", CLIENT_ID,
                "scope", SCOPE,
                "grant_type", "refresh_token",
                "refresh_token", refreshToken));
        if (!token.has("access_token")) {
            throw new AuthException(token.has("error") ? token.get("error").getAsString() : "refresh failed");
        }
        return token;
    }

    // ---- Xbox Live and Minecraft ----

    private static JsonObject minecraftLogin(String msaToken) throws IOException, InterruptedException {
        JsonObject xbl = postJson("https://user.auth.xboxlive.com/user/authenticate", """
                {"Properties":{"AuthMethod":"RPS","SiteName":"user.auth.xboxlive.com","RpsTicket":%s},
                 "RelyingParty":"http://auth.xboxlive.com","TokenType":"JWT"}""".formatted(GSON.toJson(msaToken)));

        JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", """
                {"Properties":{"SandboxId":"RETAIL","UserTokens":[%s]},
                 "RelyingParty":"rp://api.minecraftservices.com/","TokenType":"JWT"}"""
                .formatted(GSON.toJson(xbl.get("Token").getAsString())));
        String uhs = xsts.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

        JsonObject mc = postJson("https://api.minecraftservices.com/authentication/login_with_xbox",
                "{\"identityToken\":" + GSON.toJson("XBL3.0 x=" + uhs + ";" + xsts.get("Token").getAsString()) + "}");
        String accessToken = mc.get("access_token").getAsString();

        HttpResponse<String> profileResponse = send(HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build());
        if (profileResponse.statusCode() == 404) {
            throw new AuthException("this Microsoft account does not own Minecraft Java Edition");
        }
        JsonObject profile = parse(profileResponse, "Minecraft profile");

        JsonObject result = new JsonObject();
        result.addProperty("username", profile.get("name").getAsString());
        result.addProperty("uuid", profile.get("id").getAsString());
        result.addProperty("accessToken", accessToken);
        result.addProperty("expiresAt", Instant.now().plusSeconds(mc.get("expires_in").getAsLong()).getEpochSecond());
        String xuid = xuid(xsts);
        if (xuid != null) result.addProperty("xuid", xuid);
        return result;
    }

    private static String xuid(JsonObject xsts) {
        JsonArray xui = xsts.getAsJsonObject("DisplayClaims").getAsJsonArray("xui");
        JsonObject claims = xui.get(0).getAsJsonObject();
        return claims.has("xid") ? claims.get("xid").getAsString() : null;
    }

    // ---- HTTP ----

    private static JsonObject postForm(String url, Map<String, String> form) throws IOException, InterruptedException {
        return parse(send(formRequest(url, form)), url);
    }

    /** Like {@link #postForm} but returns OAuth error bodies instead of throwing. */
    private static JsonObject postFormRaw(String url, Map<String, String> form) throws IOException, InterruptedException {
        return GSON.fromJson(send(formRequest(url, form)).body(), JsonObject.class);
    }

    private static HttpRequest formRequest(String url, Map<String, String> form) {
        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private static JsonObject postJson(String url, String json) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        HttpResponse<String> response = send(request.build());
        if (response.statusCode() == 401 && url.contains("xsts")) {
            JsonObject error = GSON.fromJson(response.body(), JsonObject.class);
            long code = error != null && error.has("XErr") ? error.get("XErr").getAsLong() : 0;
            throw new AuthException(switch ((int) (code - 2148916000L)) {
                case 233 -> "this Microsoft account has no Xbox profile, sign in once at minecraft.net first";
                case 235 -> "Xbox Live is not available in this account's country";
                case 236, 237 -> "this account needs adult verification on xbox.com";
                case 238 -> "this is a child account, it must be added to a Microsoft family first";
                default -> "Xbox authorization failed (XErr " + code + ")";
            });
        }
        return parse(response, url);
    }

    private static HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject parse(HttpResponse<String> response, String what) {
        if (response.statusCode() / 100 != 2) {
            throw new AuthException(what + " returned HTTP " + response.statusCode());
        }
        return GSON.fromJson(response.body(), JsonObject.class);
    }

    // ---- cache ----

    private static JsonObject readCache(Path file) {
        try {
            return Files.isRegularFile(file) ? GSON.fromJson(Files.readString(file), JsonObject.class) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeCache(Path file, JsonObject data) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(data));
        try {
            Files.setPosixFilePermissions(tmp, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // Windows: the file lives in the user profile, which is already private.
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static final class AuthException extends IllegalStateException {
        AuthException(String message) {
            super(message);
        }
    }
}
