package com.antigravity.geminiwear;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private LinearLayout btnModelAuto, btnModel38, btnModel37, btnModel31Pro, btnModel20;
    private TextView tvModelAutoCheck, tvModel38Check, tvModel37Check, tvModel31ProCheck, tvModel20Check;

    private LinearLayout btnThinkingOff, btnThinkingLow, btnThinkingMedium, btnThinkingHigh, btnThinkingExtended;
    private TextView tvThinkingOffCheck, tvThinkingLowCheck, tvThinkingMediumCheck, tvThinkingHighCheck, tvThinkingExtendedCheck;

    private LinearLayout btnAutoUpdateToggle;
    private TextView tvAutoUpdateStatus;

    private EditText etApiKey;
    private TextView tvCurrentKeyStatus;
    private Button btnSave;

    private String currentSelectedModel;
    private int currentThinkingBudget;
    private boolean currentAutoUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        currentSelectedModel = ModelConfig.getSelectedModel(this);
        currentThinkingBudget = ModelConfig.getThinkingBudget(this);
        currentAutoUpdate = ModelConfig.isAutoUpdateEnabled(this);

        initViews();
        setupTouchFeedback();
        updateUI();

        // Check for model updates in background on settings open
        GeminiApiClient.checkForModelUpdates(this, ApiKeyStore.getApiKey(this), new GeminiApiClient.ModelsDiscoveryCallback() {
            @Override
            public void onDiscovered(String newestModel, boolean isNewer) {
                if (isNewer && !isFinishing()) {
                    Toast.makeText(SettingsActivity.this, "Wykryto nowy model: " + newestModel, Toast.LENGTH_SHORT).show();
                    updateUI();
                }
            }
        });
    }

    private void initViews() {
        btnModelAuto = findViewById(R.id.btnModelAuto);
        btnModel38 = findViewById(R.id.btnModel38);
        btnModel37 = findViewById(R.id.btnModel37);
        btnModel31Pro = findViewById(R.id.btnModel31Pro);
        btnModel20 = findViewById(R.id.btnModel20);

        tvModelAutoCheck = findViewById(R.id.tvModelAutoCheck);
        tvModel38Check = findViewById(R.id.tvModel38Check);
        tvModel37Check = findViewById(R.id.tvModel37Check);
        tvModel31ProCheck = findViewById(R.id.tvModel31ProCheck);
        tvModel20Check = findViewById(R.id.tvModel20Check);

        btnThinkingOff = findViewById(R.id.btnThinkingOff);
        btnThinkingLow = findViewById(R.id.btnThinkingLow);
        btnThinkingMedium = findViewById(R.id.btnThinkingMedium);
        btnThinkingHigh = findViewById(R.id.btnThinkingHigh);
        btnThinkingExtended = findViewById(R.id.btnThinkingExtended);

        tvThinkingOffCheck = findViewById(R.id.tvThinkingOffCheck);
        tvThinkingLowCheck = findViewById(R.id.tvThinkingLowCheck);
        tvThinkingMediumCheck = findViewById(R.id.tvThinkingMediumCheck);
        tvThinkingHighCheck = findViewById(R.id.tvThinkingHighCheck);
        tvThinkingExtendedCheck = findViewById(R.id.tvThinkingExtendedCheck);

        btnAutoUpdateToggle = findViewById(R.id.btnAutoUpdateToggle);
        tvAutoUpdateStatus = findViewById(R.id.tvAutoUpdateStatus);

        etApiKey = findViewById(R.id.etApiKey);
        tvCurrentKeyStatus = findViewById(R.id.tvCurrentKeyStatus);
        btnSave = findViewById(R.id.btnSave);

        // Model selection listeners
        btnModelAuto.setOnClickListener(v -> selectModel(ModelConfig.MODEL_AUTO));
        btnModel38.setOnClickListener(v -> selectModel(ModelConfig.MODEL_GEMINI_3_8_FLASH));
        btnModel37.setOnClickListener(v -> selectModel(ModelConfig.MODEL_GEMINI_3_7_FLASH));
        btnModel31Pro.setOnClickListener(v -> selectModel(ModelConfig.MODEL_GEMINI_3_1_PRO));
        btnModel20.setOnClickListener(v -> selectModel(ModelConfig.MODEL_GEMINI_2_0_FLASH));

        // Thinking budget listeners
        btnThinkingOff.setOnClickListener(v -> selectThinking(ModelConfig.THINKING_OFF));
        btnThinkingLow.setOnClickListener(v -> selectThinking(ModelConfig.THINKING_LOW));
        btnThinkingMedium.setOnClickListener(v -> selectThinking(ModelConfig.THINKING_MEDIUM));
        btnThinkingHigh.setOnClickListener(v -> selectThinking(ModelConfig.THINKING_HIGH));
        btnThinkingExtended.setOnClickListener(v -> selectThinking(ModelConfig.THINKING_EXTENDED));

        // Auto update toggle
        btnAutoUpdateToggle.setOnClickListener(v -> {
            currentAutoUpdate = !currentAutoUpdate;
            ModelConfig.setAutoUpdateEnabled(this, currentAutoUpdate);
            updateAutoUpdateUI();
            Toast.makeText(this, currentAutoUpdate ? "Auto-aktualizacja włączona" : "Auto-aktualizacja wyłączona", Toast.LENGTH_SHORT).show();
        });

        // Save button
        btnSave.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            if (!key.isEmpty()) {
                ApiKeyStore.setApiKey(this, key);
            }
            Toast.makeText(this, "Zapisano ustawienia!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupTouchFeedback() {
        ViewMotionHelper.applyTouchFeedback(btnModelAuto);
        ViewMotionHelper.applyTouchFeedback(btnModel38);
        ViewMotionHelper.applyTouchFeedback(btnModel37);
        ViewMotionHelper.applyTouchFeedback(btnModel31Pro);
        ViewMotionHelper.applyTouchFeedback(btnModel20);

        ViewMotionHelper.applyTouchFeedback(btnThinkingOff);
        ViewMotionHelper.applyTouchFeedback(btnThinkingLow);
        ViewMotionHelper.applyTouchFeedback(btnThinkingMedium);
        ViewMotionHelper.applyTouchFeedback(btnThinkingHigh);
        ViewMotionHelper.applyTouchFeedback(btnThinkingExtended);

        ViewMotionHelper.applyTouchFeedback(btnAutoUpdateToggle);
        ViewMotionHelper.applyTouchFeedback(btnSave);
    }

    private void selectModel(String model) {
        currentSelectedModel = model;
        ModelConfig.setSelectedModel(this, model);
        updateModelUI();
    }

    private void selectThinking(int budget) {
        currentThinkingBudget = budget;
        ModelConfig.setThinkingBudget(this, budget);
        updateThinkingUI();
    }

    private void updateUI() {
        updateModelUI();
        updateThinkingUI();
        updateAutoUpdateUI();
        updateKeyStatus();
    }

    private void updateModelUI() {
        setChipState(btnModelAuto, tvModelAutoCheck, ModelConfig.MODEL_AUTO.equals(currentSelectedModel));
        setChipState(btnModel38, tvModel38Check, ModelConfig.MODEL_GEMINI_3_8_FLASH.equals(currentSelectedModel));
        setChipState(btnModel37, tvModel37Check, ModelConfig.MODEL_GEMINI_3_7_FLASH.equals(currentSelectedModel));
        setChipState(btnModel31Pro, tvModel31ProCheck, ModelConfig.MODEL_GEMINI_3_1_PRO.equals(currentSelectedModel));
        setChipState(btnModel20, tvModel20Check, ModelConfig.MODEL_GEMINI_2_0_FLASH.equals(currentSelectedModel));
    }

    private void updateThinkingUI() {
        setChipState(btnThinkingOff, tvThinkingOffCheck, currentThinkingBudget == ModelConfig.THINKING_OFF);
        setChipState(btnThinkingLow, tvThinkingLowCheck, currentThinkingBudget == ModelConfig.THINKING_LOW);
        setChipState(btnThinkingMedium, tvThinkingMediumCheck, currentThinkingBudget == ModelConfig.THINKING_MEDIUM);
        setChipState(btnThinkingHigh, tvThinkingHighCheck, currentThinkingBudget == ModelConfig.THINKING_HIGH);
        setChipState(btnThinkingExtended, tvThinkingExtendedCheck, currentThinkingBudget == ModelConfig.THINKING_EXTENDED);
    }

    private void setChipState(LinearLayout container, TextView checkView, boolean selected) {
        container.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        checkView.setText(selected ? "✓" : "");
    }

    private void updateAutoUpdateUI() {
        tvAutoUpdateStatus.setText(currentAutoUpdate ? "WŁ" : "WYŁ");
        tvAutoUpdateStatus.setTextColor(currentAutoUpdate ? 0xFF4E88FF : 0xFF888888);
    }

    private void updateKeyStatus() {
        String key = ApiKeyStore.getApiKey(this);
        if (key != null && !key.isEmpty()) {
            String masked = (key.length() > 8) ? (key.substring(0, 4) + "..." + key.substring(key.length() - 4)) : "Aktywny";
            tvCurrentKeyStatus.setText("Klucz: " + masked);
            tvCurrentKeyStatus.setTextColor(0xFF4E88FF);
        } else {
            tvCurrentKeyStatus.setText("Status: Brak klucza");
            tvCurrentKeyStatus.setTextColor(0xFFFF5252);
        }
    }
}
