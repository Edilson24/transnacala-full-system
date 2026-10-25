package com.transnacala.app.network;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.URI;

public final class ApiConfig {
    private static final String PREFS = "transnacala_settings";
    private static final String BASE_URL = "api_base_url";

    private ApiConfig() {
    }

    public static String getBaseUrl(Context context) {
        return preferences(context).getString(BASE_URL, "");
    }

    public static void setBaseUrl(Context context, String value) {
        String normalized = value.trim().replaceAll("/+$", "");
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Endereço da API inválido.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty()
                && !"/".equals(uri.getPath()))) {
            throw new IllegalArgumentException(
                    "Use um endereço HTTPS válido, sem parâmetros ou fragmentos."
            );
        }
        preferences(context).edit().putString(BASE_URL, normalized).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
