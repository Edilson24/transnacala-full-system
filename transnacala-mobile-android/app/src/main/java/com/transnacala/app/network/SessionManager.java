package com.transnacala.app.network;

import android.content.Context;
import org.json.JSONObject;

public final class SessionManager {
    private static volatile String token = "";
    private static volatile String userId = "";
    private static volatile String role = "";
    private static volatile String fullName = "";
    private static volatile String username = "";
    private static volatile String activeSessionId = "";

    private SessionManager() {
    }

    public static void saveDriverSession(Context context, String token, JSONObject user) {
        SessionManager.token = token;
        userId = user.optString("id");
        role = user.optString("role");
        fullName = user.optString("full_name");
        username = user.optString("username");
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

    public static String getFullName(Context context) {
        return fullName;
    }

    public static String getUsername(Context context) {
        return username;
    }

    public static String getActiveSessionId(Context context) {
        return activeSessionId;
    }

    public static void setActiveSessionId(Context context, String value) {
        activeSessionId = value == null ? "" : value;
    }

    public static void clear(Context context) {
        token = "";
        userId = "";
        role = "";
        fullName = "";
        username = "";
        activeSessionId = "";
    }
}
