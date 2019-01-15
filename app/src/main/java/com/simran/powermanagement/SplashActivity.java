package com.simran.powermanagement;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.OnCompositionLoadedListener;
import com.amazonaws.youruserpools.SignIn;

import java.util.Objects;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_screen);

        LottieAnimationView animationView = (LottieAnimationView) findViewById(R.id.animation_view);
        lottieProgressConfig(animationView);

    }

    int animationLoop = 0;

    private void lottieProgressConfig(final LottieAnimationView animationView) {


        animationView.setAnimation("material_wave_loading.json");
        animationView.playAnimation();

        animationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                if (animationLoop == 0) {
                    animationLoop = 0;

                    Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    animationView.setProgress(0);
                    animationView.playAnimation();
                    animationLoop++;
                }

            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

}
