package com.antigravity.geminiwear;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;

public class GeminiVoiceSessionService extends VoiceInteractionSessionService {
    @Override
    public VoiceInteractionSession onNewSession(Bundle args) {
        return new GeminiSession(this);
    }

    private static class GeminiSession extends VoiceInteractionSession {
        public GeminiSession(Context context) {
            super(context);
        }

        @Override
        public void onShow(Bundle args, int showFlags) {
            super.onShow(args, showFlags);
            launchGeminiActivity();
            hide();
        }

        @Override
        public void onHandleAssist(Bundle data, AssistStructure structure, AssistContent content) {
            super.onHandleAssist(data, structure, content);
            launchGeminiActivity();
            hide();
        }

        private void launchGeminiActivity() {
            try {
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.setAction(Intent.ACTION_ASSIST);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intent);
            } catch (Exception ignored) {}
        }
    }
}
