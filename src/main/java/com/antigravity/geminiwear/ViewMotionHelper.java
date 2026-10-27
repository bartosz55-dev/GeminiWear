package com.antigravity.geminiwear;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

public class ViewMotionHelper {
    // Emil Kowalski strong ease-out curve: cubic-bezier(0.23, 1, 0.32, 1)
    public static final Interpolator EASE_OUT = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            ? new PathInterpolator(0.23f, 1f, 0.32f, 1f)
            : new android.view.animation.DecelerateInterpolator();

    /**
     * Attaches tactile press-scale feedback to a View (scale to 0.96 on press, 1.0 on release).
     */
    public static void applyTouchFeedback(final View view) {
        if (view == null) return;

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate()
                                .scaleX(0.96f)
                                .scaleY(0.96f)
                                .setDuration(100)
                                .setInterpolator(EASE_OUT)
                                .start();
                        performLightHaptic(v.getContext());
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(160)
                                .setInterpolator(EASE_OUT)
                                .start();
                        break;
                }
                return false; // let click listeners execute
            }
        });
    }

    /**
     * Natural entrance animation: starts at scale(0.95) and alpha(0), scales to 1.0 in 180ms.
     */
    public static void animateEntrance(View view) {
        if (view == null) return;
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.setAlpha(0f);
        view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(180)
                .setInterpolator(EASE_OUT)
                .start();
    }

    private static void performLightHaptic(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else {
                    vibrator.vibrate(12);
                }
            }
        } catch (Exception ignored) {}
    }
}
