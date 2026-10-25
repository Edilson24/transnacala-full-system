package com.transnacala.app.network;

import android.content.Context;
import org.json.JSONObject;

public final class SessionManager {
    private static volatile String token = "";
    private static volatile String userId = "";
    private static volatile String role = "";

    private SessionManager() {
    }

    public static void saveDriverSession(Context context, String token, JSONObject user) {
        SessionManager.token = token;
        userId = user.optString("id");
        role = user.optString("role");
    }

    public static String getToken(Context context) {
        return token;
    }

    public static String getUserId(Context context) {
        return userId;
    }

    public static boolean isDriverSignedIn(Context context) {
        return "DRIVER".equals(role);
    }

    public static void clear(Context context) {
        token = "";
        userId = "";
        role = "";
    }
}
