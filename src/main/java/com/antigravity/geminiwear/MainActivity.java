package com.antigravity.geminiwear;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "GeminiWearMain";
    private static final int REQ_SPEECH_INPUT = 1002;
    private static final int REQ_PICK_IMAGE = 1003;

    private ScrollView scrollChat;
    private LinearLayout llChatMessages;
    private TextView tvEmptyPrompt;

    // Model & Thinking header badge
    private LinearLayout btnHeaderModelBadge;
    private TextView tvHeaderModel;
    private TextView tvHeaderThinking;

    // Multimodal & Input elements
    private LinearLayout layoutAttachedImage;
    private ImageView ivAttachedPreview;
    private ImageView btnRemoveImage;
    private LinearLayout layoutTextInput;
    private EditText etPromptInput;
    private ImageView btnSendText;

    // Action buttons
    private ImageView btnActionMic;
    private ImageView btnActionKeyboard;
    private ImageView btnActionPhoto;
    private ImageView btnClearChat;
    private ImageView btnSettings;
    private ImageView ivGlowOrb;

    private TextToSpeech tts;
    private Vibrator vibrator;
    private ObjectAnimator orbPulseAnim;

    private final List<ChatMessage> chatList = new ArrayList<>();
    private String currentBase64Image = null;
    private Bitmap currentImageBitmap = null;
    private View currentThinkingView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initViews();
        setupTouchFeedback();
        setupAnimations();
        setupTts();
        setupClickListeners();

        // Check for model updates in background on start
        String apiKey = ApiKeyStore.getApiKey(this);
        GeminiApiClient.checkForModelUpdates(this, apiKey, new GeminiApiClient.ModelsDiscoveryCallback() {
            @Override
            public void onDiscovered(String newestModel, boolean isNewer) {
                if (isNewer) {
                    updateHeaderModelBadge();
                }
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeaderModelBadge();
    }

    private void updateHeaderModelBadge() {
        String effectiveModel = ModelConfig.getEffectiveModel(this);
        String selectedModel = ModelConfig.getSelectedModel(this);
        String displayName;

        if (ModelConfig.MODEL_AUTO.equals(selectedModel)) {
            displayName = "Auto (" + ModelConfig.getModelDisplayName(effectiveModel).replace("Gemini ", "") + ")";
        } else {
            displayName = ModelConfig.getModelDisplayName(effectiveModel).replace("Gemini ", "");
        }

        tvHeaderModel.setText(displayName);
        tvHeaderThinking.setText("• " + ModelConfig.getThinkingLevelName(this));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_ASSIST.equals(action) || Intent.ACTION_VOICE_COMMAND.equals(action)) {
            startVoiceInput();
        }
    }

    private void initViews() {
        scrollChat = findViewById(R.id.scrollChat);
        llChatMessages = findViewById(R.id.llChatMessages);
        tvEmptyPrompt = findViewById(R.id.tvEmptyPrompt);
        ivGlowOrb = findViewById(R.id.ivGlowOrb);

        btnHeaderModelBadge = findViewById(R.id.btnHeaderModelBadge);
        tvHeaderModel = findViewById(R.id.tvHeaderModel);
        tvHeaderThinking = findViewById(R.id.tvHeaderThinking);

        layoutAttachedImage = findViewById(R.id.layoutAttachedImage);
        ivAttachedPreview = findViewById(R.id.ivAttachedPreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);

        layoutTextInput = findViewById(R.id.layoutTextInput);
        etPromptInput = findViewById(R.id.etPromptInput);
        btnSendText = findViewById(R.id.btnSendText);

        btnActionMic = findViewById(R.id.btnActionMic);
        btnActionKeyboard = findViewById(R.id.btnActionKeyboard);
        btnActionPhoto = findViewById(R.id.btnActionPhoto);
        btnClearChat = findViewById(R.id.btnClearChat);
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void setupTouchFeedback() {
        ViewMotionHelper.applyTouchFeedback(btnHeaderModelBadge);
        ViewMotionHelper.applyTouchFeedback(btnClearChat);
        ViewMotionHelper.applyTouchFeedback(btnSettings);
        ViewMotionHelper.applyTouchFeedback(btnActionMic);
        ViewMotionHelper.applyTouchFeedback(btnActionKeyboard);
        ViewMotionHelper.applyTouchFeedback(btnActionPhoto);
        ViewMotionHelper.applyTouchFeedback(btnSendText);
        ViewMotionHelper.applyTouchFeedback(btnRemoveImage);
    }

    private void setupAnimations() {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.90f, 1.15f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.90f, 1.15f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.12f, 0.28f);

        orbPulseAnim = ObjectAnimator.ofPropertyValuesHolder(ivGlowOrb, scaleX, scaleY, alpha);
        orbPulseAnim.setDuration(2200);
        orbPulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        orbPulseAnim.setRepeatMode(ValueAnimator.REVERSE);
        orbPulseAnim.setInterpolator(ViewMotionHelper.EASE_OUT);
        orbPulseAnim.start();
    }

    private void setupTts() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int res = tts.setLanguage(new Locale("pl", "PL"));
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.getDefault());
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        btnHeaderModelBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTts();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        btnClearChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearChatHistory();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTts();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        btnActionMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                layoutTextInput.setVisibility(View.GONE);
                startVoiceInput();
            }
        });

        btnActionKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleKeyboardInput();
            }
        });

        btnActionPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTts();
                openImagePicker();
            }
        });

        btnRemoveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAttachedImage();
            }
        });

        btnSendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitTextInput();
            }
        });

        etPromptInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                    submitTextInput();
                    return true;
                }
                return false;
            }
        });
    }

    private void startVoiceInput() {
        stopTts();
        vibrateHaptic(30);

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pl-PL");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Mów do Gemini...");
            startActivityForResult(intent, REQ_SPEECH_INPUT);
        } catch (Exception e) {
            Log.e(TAG, "Speech intent failed", e);
            Toast.makeText(this, "Wpisz pytanie na klawiaturze", Toast.LENGTH_SHORT).show();
            toggleKeyboardInput();
        }
    }

    private void toggleKeyboardInput() {
        stopTts();
        if (layoutTextInput.getVisibility() == View.VISIBLE) {
            hideKeyboard();
            layoutTextInput.setVisibility(View.GONE);
        } else {
            layoutTextInput.setVisibility(View.VISIBLE);
            etPromptInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etPromptInput, InputMethodManager.SHOW_IMPLICIT);
            }
            scrollToBottom();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etPromptInput.getWindowToken(), 0);
        }
    }

    private void submitTextInput() {
        String text = etPromptInput.getText().toString().trim();
        if (text.isEmpty() && currentBase64Image == null) {
            Toast.makeText(this, "Wpisz pytanie", Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();
        layoutTextInput.setVisibility(View.GONE);
        etPromptInput.setText("");

        onUserQuerySubmitted(text);
    }

    private void openImagePicker() {
        Intent intent = new Intent(this, ImagePickerActivity.class);
        startActivityForResult(intent, REQ_PICK_IMAGE);
    }

    private void clearAttachedImage() {
        currentBase64Image = null;
        currentImageBitmap = null;
        layoutAttachedImage.setVisibility(View.GONE);
        ivAttachedPreview.setImageDrawable(null);
    }

    private void clearChatHistory() {
        chatList.clear();
        llChatMessages.removeAllViews();
        tvEmptyPrompt.setVisibility(View.VISIBLE);
        llChatMessages.addView(tvEmptyPrompt);
        clearAttachedImage();
        stopTts();
        Toast.makeText(this, "Nowy czat", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                onUserQuerySubmitted(matches.get(0));
            }
        } else if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            String imagePath = data.getStringExtra(ImagePickerActivity.EXTRA_IMAGE_PATH);
            if (imagePath != null) {
                processSelectedImage(imagePath);
            } else if (data.getData() != null) {
                processSelectedImageUri(data.getData());
            }
        }
    }

    private void processSelectedImage(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);

            int maxDim = Math.max(opts.outWidth, opts.outHeight);
            opts.inSampleSize = Math.max(1, maxDim / 600);
            opts.inJustDecodeBounds = false;

            Bitmap bm = BitmapFactory.decodeFile(path, opts);
            if (bm != null) {
                attachBitmapToQuery(bm);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            Toast.makeText(this, "Błąd zdjęcia", Toast.LENGTH_SHORT).show();
        }
    }

    private void processSelectedImageUri(android.net.Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
            Bitmap bm = BitmapFactory.decodeStream(is, null, opts);
            is.close();

            if (bm != null) {
                attachBitmapToQuery(bm);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from Uri", e);
            Toast.makeText(this, "Błąd zdjęcia", Toast.LENGTH_SHORT).show();
        }
    }

    private void attachBitmapToQuery(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] bytes = baos.toByteArray();
        currentBase64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);
        currentImageBitmap = bm;

        ivAttachedPreview.setImageBitmap(bm);
        layoutAttachedImage.setVisibility(View.VISIBLE);
        ViewMotionHelper.animateEntrance(layoutAttachedImage);
        vibrateHaptic(30);
        Toast.makeText(this, "Zdjęcie dołączone! Zadaj pytanie.", Toast.LENGTH_SHORT).show();
    }

    private void onUserQuerySubmitted(String query) {
        tvEmptyPrompt.setVisibility(View.GONE);

        // Add user message to history
        ChatMessage userMsg = new ChatMessage(ChatMessage.ROLE_USER, query, currentBase64Image);
        chatList.add(userMsg);

        // Render user message in chat with entrance animation
        renderUserMessage(query, currentImageBitmap);

        // Reset attachment after sending
        clearAttachedImage();

        // Show thinking indicator
        showThinkingIndicator();
        scrollToBottom();

        // Call Gemini with full history and context
        String apiKey = ApiKeyStore.getApiKey(this);
        GeminiApiClient.askGeminiHistory(this, apiKey, chatList, new GeminiApiClient.ResponseCallback() {
            @Override
            public void onSuccess(String reply) {
                hideThinkingIndicator();
                ChatMessage modelMsg = new ChatMessage(ChatMessage.ROLE_MODEL, reply);
                chatList.add(modelMsg);
                renderModelMessage(reply);
                speakAnswer(reply);
                vibrateHaptic(35);
                scrollToBottom();
            }

            @Override
            public void onError(String errorMessage) {
                hideThinkingIndicator();
                renderErrorMessage(errorMessage);
                vibrateHaptic(75);
                scrollToBottom();
            }
        });
    }

    private void renderUserMessage(String text, Bitmap image) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 5, 0, 5);
        row.setLayoutParams(lp);

        if (image != null) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(image);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(100, 100);
            ivLp.gravity = Gravity.END;
            ivLp.setMargins(0, 0, 0, 4);
            iv.setLayoutParams(ivLp);
            row.addView(iv);
        }

        if (text != null && !text.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(12);
            tv.setMaxLines(Integer.MAX_VALUE);
            tv.setSingleLine(false);
            tv.setHorizontallyScrolling(false);
            tv.setBackgroundResource(R.drawable.bg_bubble_user);
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tvLp.gravity = Gravity.END;
            tv.setLayoutParams(tvLp);
            row.addView(tv);
        }

        llChatMessages.addView(row);
        ViewMotionHelper.animateEntrance(row);
    }

    private void renderModelMessage(final String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.START);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 5, 0, 5);
        row.setLayoutParams(lp);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(12);
        tv.setMaxLines(Integer.MAX_VALUE);
        tv.setSingleLine(false);
        tv.setHorizontallyScrolling(false);
        tv.setLineSpacing(4f, 1.15f);
        tv.setBackgroundResource(R.drawable.bg_bubble_gemini);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tvLp.gravity = Gravity.START;
        tv.setLayoutParams(tvLp);

        // Tapping speech bubble re-reads it
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakAnswer(text);
            }
        });

        row.addView(tv);
        llChatMessages.addView(row);
        ViewMotionHelper.animateEntrance(row);
    }

    private void renderErrorMessage(String error) {
        TextView tv = new TextView(this);
        tv.setText("⚠️ " + error);
        tv.setTextColor(0xFFFF6B6B);
        tv.setTextSize(11);
        tv.setMaxLines(Integer.MAX_VALUE);
        tv.setSingleLine(false);
        tv.setBackgroundResource(R.drawable.bg_bubble_gemini);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 5, 0, 5);
        tv.setLayoutParams(lp);
        llChatMessages.addView(tv);
        ViewMotionHelper.animateEntrance(tv);
    }

    private void showThinkingIndicator() {
        if (currentThinkingView != null) {
            llChatMessages.removeView(currentThinkingView);
        }

        String effectiveModel = ModelConfig.getModelDisplayName(ModelConfig.getEffectiveModel(this)).replace("Gemini ", "");
        String thinkingLevel = ModelConfig.getThinkingLevelName(this);

        TextView tv = new TextView(this);
        tv.setText("⏳ " + effectiveModel + " analizuje (" + thinkingLevel + ")...");
        tv.setTextColor(0xFF4E88FF);
        tv.setTextSize(10);
        tv.setBackgroundResource(R.drawable.bg_bubble_gemini);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 5, 0, 5);
        tv.setLayoutParams(lp);

        currentThinkingView = tv;
        llChatMessages.addView(tv);
        ViewMotionHelper.animateEntrance(tv);
    }

    private void hideThinkingIndicator() {
        if (currentThinkingView != null) {
            llChatMessages.removeView(currentThinkingView);
            currentThinkingView = null;
        }
    }

    private void scrollToBottom() {
        scrollChat.post(new Runnable() {
            @Override
            public void run() {
                scrollChat.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void speakAnswer(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gemini_tts_" + System.currentTimeMillis());
        }
    }

    private void stopTts() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    private void vibrateHaptic(int durationMs) {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
    }
}
