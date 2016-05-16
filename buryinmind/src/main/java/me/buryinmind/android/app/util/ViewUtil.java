package me.buryinmind.android.app.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

/**
 * Created by jasontujun on 2016/5/15.
 */
public class ViewUtil {

    public static void animateFadeInOut(final View view, final boolean fadeout) {
        view.setVisibility(fadeout ? View.GONE : View.VISIBLE);
        view.animate().setDuration(200).alpha(
                fadeout ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(fadeout ? View.GONE : View.VISIBLE);
            }
        });
    }
}
