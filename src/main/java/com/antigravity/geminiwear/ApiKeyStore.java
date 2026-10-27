package com.antigravity.geminiwear;

import android.content.Context;
import android.content.SharedPreferences;

public class ApiKeyStore {
    private static final String PREF_NAME = "gemini_wear_prefs";
    private static final String KEY_API_KEY = "gemini_api_key";
    public static final String DEFAULT_API_KEY = "AQ.Ab8RN6K9Lnlzujgh7MEc1Tu5LRS059YvDjFF5h9zNyl_-VAA1Q";

    public static void setApiKey(Context context, String key) {
        if (key != null) {
            key = key.trim();
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_API_KEY, key).commit();
    }

    public static String getApiKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_API_KEY, null);
        if (saved != null && !saved.trim().isEmpty()) {
            return saved.trim();
        }
        return DEFAULT_API_KEY;
    }

    public static boolean hasApiKey(Context context) {
        String key = getApiKey(context);
        return key != null && !key.trim().isEmpty();
    }
}
