package me.buryinmind.android.app.uicontrol;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Created by jasontujun on 2016/5/23.
 */
public class ScaleBehavior extends CoordinatorLayout.Behavior<View> {

    private Interpolator interpolator;

    public ScaleBehavior() {
        super();
        init();
    }

    public ScaleBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        interpolator = new AccelerateInterpolator(2);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
            float mRatio = Math.abs(dependency.getY() / ((AppBarLayout) dependency).getTotalScrollRange());
            float ratio = 1 - interpolator.getInterpolation(mRatio);
            child.setScaleX(ratio);
            child.setScaleY(ratio);
            child.setAlpha(ratio);
            return true;
        }
        return false;
    }
}
