package com.transnacala.app.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ApiClient {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private final String baseUrl;
    private final String accessToken;

    public ApiClient(String baseUrl, String accessToken) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.accessToken = accessToken;
    }

    public JSONObject get(String path) throws IOException {
        return request("GET", path, null);
    }

    public JSONObject post(String path, JSONObject body) throws IOException {
        return request("POST", path, body);
    }

    public JSONObject put(String path, JSONObject body) throws IOException {
        return request("PUT", path, body);
    }

    public JSONObject delete(String path) throws IOException {
        return request("DELETE", path, null);
    }

    private JSONObject request(String method, String path, JSONObject body) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("bypass-tunnel-reminder", "true");
            if (accessToken != null && !accessToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload);
                }
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String response = readBody(connection.getErrorStream());
                throw ApiException.fromResponse(status, response);
            }
            if (status == HttpURLConnection.HTTP_NO_CONTENT) {
                return null;
            }
            String response = readBody(connection.getInputStream());
            return new JSONObject(response);
        } catch (JSONException exception) {
            throw new IOException("A API devolveu uma resposta JSON inválida.", exception);
        } catch (java.net.MalformedURLException exception) {
            throw new IOException("O endereço da API é inválido.", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    public static final class ApiException extends IOException {
        private final int statusCode;
        private final String errorCode;

        private ApiException(int statusCode, String errorCode, String message) {
            super(message);
            this.statusCode = statusCode;
            this.errorCode = errorCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

        private static ApiException fromResponse(int status, String response) {
            String message = status == 511
                    ? "A rede ou o túnel exige autenticação (HTTP 511). "
                    + "Confirme que o LocalTunnel está ativo ou teste outra rede."
                    : "Falha de comunicação com a API (HTTP " + status + ").";
            String code = "";
            try {
                JSONObject error = new JSONObject(response).optJSONObject("error");
                if (error != null) {
                    code = error.optString("code", "");
                    message = error.optString("message", message);
                }
            } catch (JSONException ignored) {
                // Error bodies are not guaranteed to be JSON.
            }
            return new ApiException(status, code, message);
        }
    }
}
