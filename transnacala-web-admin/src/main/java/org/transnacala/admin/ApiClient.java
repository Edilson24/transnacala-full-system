package org.transnacala.admin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

final class ApiClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final String baseUrl;
    private final String token;

    ApiClient(String token) {
        this(
                System.getenv().getOrDefault(
                        "TRANSNACALA_API_URL", "http://127.0.0.1:8000"),
                token);
    }

    ApiClient(String configuredBaseUrl, String token) {
        URI base = URI.create(configuredBaseUrl);
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getUserInfo() != null) {
            throw new IllegalStateException("TRANSNACALA_API_URL must be an HTTP(S) URL.");
        }
        this.baseUrl = configuredBaseUrl.replaceAll("/+$", "");
        this.token = token;
    }

    JsonObject login(String username, String password)
            throws IOException, InterruptedException {
        JsonObject response = send("POST", "/api/v1/auth/login",
                Map.of("username", username, "password", password), false);
        return response;
    }

    JsonObject get(String path) throws IOException, InterruptedException {
        return send("GET", path, null, true);
    }

    JsonObject post(String path, Object payload)
            throws IOException, InterruptedException {
        return send("POST", path, payload, true);
    }

    JsonObject patch(String path, Object payload)
            throws IOException, InterruptedException {
        return send("PATCH", path, payload, true);
    }

    JsonObject put(String path, Object payload)
            throws IOException, InterruptedException {
        return send("PUT", path, payload, true);
    }

    private JsonObject send(String method, String path, Object payload, boolean authenticated)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
        if (authenticated) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (payload == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json; charset=utf-8")
                    .method(method, HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)));
        }
        HttpResponse<String> response = HTTP.send(
                builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonObject body = parseResponse(response.body(), response.statusCode());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String code = "API_REQUEST_FAILED";
            String message = "A API devolveu HTTP " + response.statusCode() + ".";
            if (body.has("error") && body.get("error").isJsonObject()) {
                JsonObject error = body.getAsJsonObject("error");
                if (error.has("code") && error.get("code").isJsonPrimitive()) {
                    code = error.get("code").getAsString();
                }
                if (error.has("message") && error.get("message").isJsonPrimitive()) {
                    message = error.get("message").getAsString();
                }
            }
            throw new ApiException(message, response.statusCode(), code);
        }
        return body;
    }

    private static JsonObject parseResponse(String responseBody, int statusCode) {
        if (responseBody == null || responseBody.isBlank()) {
            if (statusCode >= 200 && statusCode < 300) {
                return new JsonObject();
            }
            throw new ApiException(
                    "A API devolveu HTTP " + statusCode + " sem uma resposta JSON.",
                    statusCode,
                    "INVALID_API_RESPONSE");
        }
        try {
            JsonElement parsed = JsonParser.parseString(responseBody);
            if (parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (JsonParseException | IllegalStateException ignored) {
            // A proxy, tunnel or unhandled server error can return HTML or plain text.
        }
        throw new ApiException(
                "A API devolveu uma resposta que não é um objeto JSON (HTTP "
                        + statusCode + "). Confirme se TRANSNACALA_API_URL aponta "
                        + "para a API e não para o Tomcat.",
                statusCode,
                "INVALID_API_RESPONSE");
    }

    static final class ApiException extends RuntimeException {
        private final int statusCode;
        private final String code;

        ApiException(String message, int statusCode, String code) {
            super(message);
            this.statusCode = statusCode;
            this.code = code;
        }

        int getStatusCode() {
            return statusCode;
        }

        String getCode() {
            return code;
        }
    }
}
