package com.example.dnervecairo.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.BuildConfig;
import com.example.dnervecairo.MainActivity;
import com.example.dnervecairo.R;
import com.example.dnervecairo.utils.PreferenceManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000;

    private ImageView ivLogo;
    private TextView tvAppName;
    private TextView tvTagline;
    private TextView tvVersion;
    private TextView tvLoadingText;
    private ProgressBar progressBar;
    private View decorTop, decorBottom;
    private View glowEffect;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int loadingStep = 0;
    private final String[] loadingMessages = {
        "Initializing...",
        "Loading routes...",
        "Preparing dashboard...",
        "Almost ready..."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        startAnimations();
        startLoadingTextAnimation();
        navigateAfterDelay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void initViews() {
        ivLogo = findViewById(R.id.iv_logo);
        tvAppName = findViewById(R.id.tv_app_name);
        tvTagline = findViewById(R.id.tv_tagline);
        tvVersion = findViewById(R.id.tv_version);
        tvLoadingText = findViewById(R.id.tv_loading_text);
        progressBar = findViewById(R.id.progress_bar);
        decorTop = findViewById(R.id.decor_top);
        decorBottom = findViewById(R.id.decor_bottom);
        glowEffect = findViewById(R.id.glow_effect);

        // Set version
        try {
            String version = "v" + BuildConfig.VERSION_NAME;
            tvVersion.setText(version);
        } catch (Exception e) {
            tvVersion.setText("v1.0.0");
        }

        // Initially invisible/prepared for animation
        ivLogo.setAlpha(0f);
        ivLogo.setScaleX(0.3f);
        ivLogo.setScaleY(0.3f);
        
        tvAppName.setAlpha(0f);
        tvAppName.setTranslationY(30f);
        
        tvTagline.setAlpha(0f);
        tvTagline.setTranslationY(20f);
        
        if (tvLoadingText != null) {
            tvLoadingText.setAlpha(0f);
        }
        
        if (progressBar != null) {
            progressBar.setAlpha(0f);
        }

        if (glowEffect != null) {
            glowEffect.setAlpha(0f);
            glowEffect.setScaleX(0.5f);
            glowEffect.setScaleY(0.5f);
        }

        // Decor elements
        if (decorTop != null) {
            decorTop.setTranslationY(-200f);
            decorTop.setAlpha(0f);
        }
        if (decorBottom != null) {
            decorBottom.setTranslationY(200f);
            decorBottom.setAlpha(0f);
        }
    }

    private void startAnimations() {
        // Decor animations (background elements slide in)
        if (decorTop != null) {
            decorTop.animate()
                .translationY(0f)
                .alpha(0.15f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
        
        if (decorBottom != null) {
            decorBottom.animate()
                .translationY(0f)
                .alpha(0.15f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }

        // Glow effect behind logo
        handler.postDelayed(() -> {
            if (glowEffect != null) {
                glowEffect.animate()
                    .alpha(0.6f)
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            }
        }, 100);

        // Logo animation - bounce in with overshoot
        handler.postDelayed(() -> {
            ivLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(700)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .withEndAction(this::startLogoPulse)
                .start();
        }, 200);

        // App name - slide up and fade in
        handler.postDelayed(() -> {
            tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }, 600);

        // Tagline - slide up and fade in
        handler.postDelayed(() -> {
            tvTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }, 800);

        // Loading indicator and text
        handler.postDelayed(() -> {
            if (progressBar != null) {
                progressBar.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
            }
            if (tvLoadingText != null) {
                tvLoadingText.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
            }
        }, 1000);
    }

    private void startLogoPulse() {
        // Subtle continuous pulse on logo
        ivLogo.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(1000)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                ivLogo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(this::startLogoPulse)
                    .start();
            })
            .start();

        // Glow pulse
        if (glowEffect != null) {
            glowEffect.animate()
                .scaleX(1.4f)
                .scaleY(1.4f)
                .alpha(0.3f)
                .setDuration(1000)
                .withEndAction(() -> {
                    glowEffect.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .alpha(0.6f)
                        .setDuration(1000)
                        .start();
                })
                .start();
        }
    }

    private void startLoadingTextAnimation() {
        if (tvLoadingText == null) return;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loadingStep < loadingMessages.length) {
                    // Fade out current text
                    tvLoadingText.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            tvLoadingText.setText(loadingMessages[loadingStep]);
                            tvLoadingText.animate()
                                .alpha(1f)
                                .setDuration(150)
                                .start();
                            loadingStep++;
                        })
                        .start();
                    
                    handler.postDelayed(this, 600);
                }
            }
        }, 1200);
    }

    private void navigateAfterDelay() {
        handler.postDelayed(() -> {
            // Fade out animation before navigation
            fadeOutAndNavigate();
        }, SPLASH_DURATION);
    }

    private void fadeOutAndNavigate() {
        // Fade out all elements
        View[] views = {ivLogo, tvAppName, tvTagline, progressBar, tvLoadingText, glowEffect};
        
        for (View view : views) {
            if (view != null) {
                view.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .start();
            }
        }

        // Scale down logo as it fades
        ivLogo.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(300)
            .start();

        handler.postDelayed(() -> {
            PreferenceManager prefManager = new PreferenceManager(this);

            Intent intent;
            if (prefManager.isLoggedIn()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 350);
    }
}
