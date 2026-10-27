package com.antigravity.geminiwear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ApiKeyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.antigravity.geminiwear.SET_API_KEY".equals(intent.getAction())) {
            String key = intent.getStringExtra("key");
            if (key != null && !key.trim().isEmpty()) {
                ApiKeyStore.setApiKey(context, key);
                Toast.makeText(context, "Klucz Gemini API został pomyślnie zapisany!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
