package me.buryinmind.android.app.util;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.tj.xengine.core.network.http.XHttpResponse;

import me.buryinmind.android.app.R;

/**
 * Created by jasontujun on 2016/5/15.
 */
public abstract class ViewUtil {

    public static void hidInputMethod(Activity activity) {
        if (activity == null)
            return;
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager mInputMethodManager = (InputMethodManager)
                    activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void animateFadeInOut(final View view, final boolean fadeout) {
        if (view == null)
            return;
        view.animate()
                .alpha(fadeout ? 0f : 1f)
                .setDuration(300);
    }

    public static Animator animateScale(final View view) {
        if (view == null)
            return null;
        ObjectAnimator anim = ObjectAnimator
                .ofFloat(view, "myScale", 1.0F, 0.7F)
                .setDuration(2000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.start();
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float cVal = (Float) animation.getAnimatedValue();
                view.setScaleX(cVal);
                view.setScaleY(cVal);
            }
        });
        return anim;
    }

    public static Animator animateShine(final View view) {
        if (view == null)
            return null;
        ObjectAnimator anim = ObjectAnimator
                .ofFloat(view, "alpha", 1.0F, 0.1F)
                .setDuration(2000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.start();
        return anim;
    }

    public static void showNetworkError(Context context) {
        Toast.makeText(context, R.string.error_network, Toast.LENGTH_SHORT).show();
    }

    public static void showApiError(Activity activity, XHttpResponse response) {
    }
}
