package com.antigravity.geminiwear;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelConfig {
    private static final String PREF_NAME = "gemini_wear_prefs";
    private static final String KEY_SELECTED_MODEL = "selected_model";
    private static final String KEY_THINKING_BUDGET = "thinking_budget";
    private static final String KEY_AUTO_UPDATE = "auto_update_models";
    private static final String KEY_LATEST_DISCOVERED = "latest_discovered_model";

    public static final String MODEL_AUTO = "auto";
    public static final String MODEL_GEMINI_3_8_FLASH = "gemini-3.8-flash";
    public static final String MODEL_GEMINI_3_7_FLASH = "gemini-3.7-flash";
    public static final String MODEL_GEMINI_3_6_FLASH = "gemini-3.6-flash";
    public static final String MODEL_GEMINI_2_0_FLASH = "gemini-2.0-flash";
    public static final String MODEL_GEMINI_3_1_PRO = "gemini-3.1-pro-preview";

    // Thinking Budget values in tokens
    public static final int THINKING_OFF = 0;           // Instant response (no thinking)
    public static final int THINKING_LOW = 1024;        // Fast thinking
    public static final int THINKING_MEDIUM = 4096;     // Balanced thinking (default)
    public static final int THINKING_HIGH = 8192;       // Deep reasoning
    public static final int THINKING_EXTENDED = 16384;  // Extended reasoning

    public static String getSelectedModel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SELECTED_MODEL, MODEL_AUTO);
    }

    public static void setSelectedModel(Context context, String model) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SELECTED_MODEL, model).apply();
    }

    public static String getLatestDiscoveredModel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LATEST_DISCOVERED, MODEL_GEMINI_3_8_FLASH);
    }

    public static void setLatestDiscoveredModel(Context context, String model) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LATEST_DISCOVERED, model).apply();
    }

    public static boolean isAutoUpdateEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_UPDATE, true);
    }

    public static void setAutoUpdateEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply();
    }

    public static String getEffectiveModel(Context context) {
        String selected = getSelectedModel(context);
        if (MODEL_AUTO.equals(selected)) {
            return getLatestDiscoveredModel(context);
        }
        return selected;
    }

    public static int getThinkingBudget(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THINKING_BUDGET, THINKING_MEDIUM);
    }

    public static void setThinkingBudget(Context context, int budget) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THINKING_BUDGET, budget).apply();
    }

    public static String getThinkingLevelName(Context context) {
        int budget = getThinkingBudget(context);
        switch (budget) {
            case THINKING_OFF:
                return "Błyskawiczne";
            case THINKING_LOW:
                return "Niskie";
            case THINKING_MEDIUM:
                return "Średnie";
            case THINKING_HIGH:
                return "Wysokie";
            case THINKING_EXTENDED:
                return "Extended";
            default:
                return budget + " tok.";
        }
    }

    public static String getModelDisplayName(String modelId) {
        if (modelId == null) return "Gemini 3.8 Flash";
        switch (modelId) {
            case MODEL_AUTO:
                return "Auto (Najnowszy)";
            case MODEL_GEMINI_3_8_FLASH:
                return "Gemini 3.8 Flash";
            case MODEL_GEMINI_3_7_FLASH:
                return "Gemini 3.7 Flash";
            case MODEL_GEMINI_3_6_FLASH:
                return "Gemini 3.6 Flash";
            case MODEL_GEMINI_2_0_FLASH:
                return "Gemini 2.0 Flash";
            case MODEL_GEMINI_3_1_PRO:
                return "Gemini 3.1 Pro";
            default:
                if (modelId.startsWith("gemini-")) {
                    String clean = modelId.substring(7);
                    return "Gemini " + clean;
                }
                return modelId;
        }
    }

    /**
     * Inspects the models array from Google API and returns the newest flash model ID found.
     */
    public static String extractNewestFlashModel(JSONArray modelsArray) {
        String bestModel = MODEL_GEMINI_3_8_FLASH;
        double bestVersion = 3.8;

        if (modelsArray == null) return bestModel;

        Pattern flashPattern = Pattern.compile("gemini-(\\d+(\\.\\d+)?)-flash");

        for (int i = 0; i < modelsArray.length(); i++) {
            JSONObject modelObj = modelsArray.optJSONObject(i);
            if (modelObj == null) continue;
            String name = modelObj.optString("name", ""); // e.g. "models/gemini-3.8-flash"
            if (name.startsWith("models/")) {
                name = name.substring(7);
            }

            Matcher m = flashPattern.matcher(name);
            if (m.find()) {
                try {
                    double ver = Double.parseDouble(m.group(1));
                    if (ver > bestVersion) {
                        bestVersion = ver;
                        bestModel = name;
                    }
                } catch (Exception ignored) {}
            }
        }
        return bestModel;
    }
}
