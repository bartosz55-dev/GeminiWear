package com.antigravity.geminiwear;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiApiClient {
    private static final String TAG = "GeminiApiClient";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ResponseCallback {
        void onSuccess(String reply);
        void onError(String errorMessage);
    }

    public interface ModelsDiscoveryCallback {
        void onDiscovered(String newestModel, boolean isNewer);
    }

    public static void askGeminiHistory(final Context context, final String apiKey, final List<ChatMessage> chatHistory, final ResponseCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Brak klucza Gemini API.");
            return;
        }
        if (chatHistory == null || chatHistory.isEmpty()) {
            callback.onError("Pusta historia wiadomości.");
            return;
        }

        final String targetModel = ModelConfig.getEffectiveModel(context);
        final int thinkingBudget = ModelConfig.getThinkingBudget(context);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String responseText = null;
                String error = null;

                // Build fallback candidate list
                List<String> modelsToTry = new ArrayList<>();
                modelsToTry.add(targetModel);
                if (!modelsToTry.contains(ModelConfig.MODEL_GEMINI_3_8_FLASH)) modelsToTry.add(ModelConfig.MODEL_GEMINI_3_8_FLASH);
                if (!modelsToTry.contains(ModelConfig.MODEL_GEMINI_3_7_FLASH)) modelsToTry.add(ModelConfig.MODEL_GEMINI_3_7_FLASH);
                if (!modelsToTry.contains(ModelConfig.MODEL_GEMINI_3_6_FLASH)) modelsToTry.add(ModelConfig.MODEL_GEMINI_3_6_FLASH);
                if (!modelsToTry.contains(ModelConfig.MODEL_GEMINI_2_0_FLASH)) modelsToTry.add(ModelConfig.MODEL_GEMINI_2_0_FLASH);

                for (String model : modelsToTry) {
                    try {
                        Log.d(TAG, "Calling Gemini model: " + model + " with thinkingBudget=" + thinkingBudget);
                        responseText = callModelWithHistory(apiKey, model, chatHistory, thinkingBudget);
                        if (responseText != null && !responseText.isEmpty()) {
                            error = null;
                            break;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Model " + model + " failed: " + e.getMessage());
                        error = e.getMessage();
                    }
                }

                final String finalResponse = responseText;
                final String finalError = error;

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalResponse != null) {
                            callback.onSuccess(finalResponse);
                        } else {
                            callback.onError(finalError != null ? finalError : "Błąd połączenia z modelem Gemini.");
                        }
                    }
                });
            }
        });
    }

    // Overload for backward compatibility
    public static void askGeminiHistory(final String apiKey, final List<ChatMessage> chatHistory, final ResponseCallback callback) {
        askGeminiHistory(null, apiKey, chatHistory, callback);
    }

    /**
     * Checks Google API in background for newest available Gemini models and auto-updates if enabled.
     */
    public static void checkForModelUpdates(final Context context, final String apiKey, final ModelsDiscoveryCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty() || context == null) return;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlString = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(12000);

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject json = new JSONObject(sb.toString());
                        JSONArray models = json.optJSONArray("models");
                        if (models != null) {
                            final String newest = ModelConfig.extractNewestFlashModel(models);
                            String current = ModelConfig.getLatestDiscoveredModel(context);
                            final boolean isNewer = !newest.equals(current);

                            if (ModelConfig.isAutoUpdateEnabled(context) && isNewer) {
                                ModelConfig.setLatestDiscoveredModel(context, newest);
                                Log.i(TAG, "Auto-updated latest model to: " + newest);
                            }

                            if (callback != null) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onDiscovered(newest, isNewer);
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error checking for model updates: " + e.getMessage());
                }
            }
        });
    }

    private static String callModelWithHistory(String apiKey, String modelName, List<ChatMessage> chatHistory, int thinkingBudget) throws Exception {
        String urlString = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(35000);
        conn.setDoOutput(true);

        JSONObject payload = new JSONObject();

        // System Instruction
        JSONObject systemInstruction = new JSONObject();
        JSONArray sysParts = new JSONArray();
        JSONObject sysPartText = new JSONObject();
        sysPartText.put("text", "Jesteś inteligentnym, pomocnym asystentem Gemini na smartwatchu Xiaomi Watch 2 z Wear OS. " +
                "Prowadzisz naturalną rozmowę z użytkownikiem i pamiętasz kontekst. " +
                "Odpowiadaj po polsku w sposób wyczerpujący, ale zwięzły i naturalny na okrągłym ekranie zegarka. " +
                "Zawsze dokończ swoją odpowiedź i myśli w całości, nigdy nie ucinaj zdań w połowie.");
        sysParts.put(sysPartText);
        systemInstruction.put("parts", sysParts);
        payload.put("system_instruction", systemInstruction);

        // Contents (Multi-turn conversation history)
        JSONArray contents = new JSONArray();
        int startIdx = Math.max(0, chatHistory.size() - 10);
        for (int i = startIdx; i < chatHistory.size(); i++) {
            ChatMessage msg = chatHistory.get(i);
            JSONObject contentObj = new JSONObject();
            contentObj.put("role", msg.isUser() ? "user" : "model");
            JSONArray parts = new JSONArray();

            if (msg.hasImage()) {
                JSONObject imgPart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mime_type", "image/jpeg");
                inlineData.put("data", msg.getBase64Image());
                imgPart.put("inline_data", inlineData);
                parts.put(imgPart);
            }

            JSONObject textPart = new JSONObject();
            textPart.put("text", (msg.getText() != null && !msg.getText().isEmpty()) ? msg.getText() : "Analiza");
            parts.put(textPart);

            contentObj.put("parts", parts);
            contents.put(contentObj);
        }
        payload.put("contents", contents);

        // Generation Config with thinking budget
        JSONObject genConfig = new JSONObject();
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 4096);

        if (thinkingBudget >= 0) {
            JSONObject thinkingConfig = new JSONObject();
            thinkingConfig.put("thinkingBudget", thinkingBudget);
            genConfig.put("thinkingConfig", thinkingConfig);
        }

        payload.put("generationConfig", genConfig);

        byte[] postData = payload.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData);
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
        }

        if (responseCode < 200 || responseCode >= 300) {
            String errBody = sb.toString();
            try {
                JSONObject errJson = new JSONObject(errBody);
                if (errJson.has("error")) {
                    JSONObject errObj = errJson.getJSONObject("error");
                    String msg = errObj.optString("message", "Kod błędu: " + responseCode);
                    throw new Exception("Błąd API (" + responseCode + "): " + msg);
                }
            } catch (Exception ignored) {}
            throw new Exception("Błąd serwera (" + responseCode + "): " + errBody);
        }

        JSONObject respJson = new JSONObject(sb.toString());
        if (respJson.has("candidates")) {
            JSONArray candidates = respJson.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                if (candidate.has("content")) {
                    JSONObject content = candidate.getJSONObject("content");
                    if (content.has("parts")) {
                        JSONArray parts = content.getJSONArray("parts");
                        StringBuilder replyBuilder = new StringBuilder();
                        for (int p = 0; p < parts.length(); p++) {
                            JSONObject part = parts.getJSONObject(p);
                            if (part.has("text")) {
                                replyBuilder.append(part.getString("text"));
                            }
                        }
                        if (replyBuilder.length() > 0) {
                            return replyBuilder.toString().trim();
                        }
                    }
                }
            }
        }

        return "Gemini nie zwróciło odpowiedzi.";
    }
}
