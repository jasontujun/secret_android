package me.buryinmind.android.app.uicontrol;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by jasontujun on 2016/5/23.
 */
public class ScaleBehavior extends CoordinatorLayout.Behavior<View> {

    public ScaleBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
            float ratio = 1 + dependency.getY() / ((AppBarLayout) dependency).getTotalScrollRange();
            child.setScaleX(ratio);
            child.setScaleY(ratio);
            return true;
        }
        return false;
    }
}
