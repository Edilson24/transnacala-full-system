package com.transnacala.app.network;

import android.content.Context;
public final class ApiConfig {
    private static final String BASE_URL = "https://transnacala-api.loca.lt";

    private ApiConfig() {
    }

    public static String getBaseUrl(Context context) {
        return BASE_URL;
    }
}
