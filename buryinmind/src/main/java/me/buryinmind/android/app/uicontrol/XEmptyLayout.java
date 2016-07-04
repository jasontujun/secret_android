package me.buryinmind.android.app.uicontrol;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Created by jasontujun on 2016/7/4.
 */
public class XEmptyLayout extends ViewGroup {
    public XEmptyLayout(Context context) {
        super(context);
    }

    public XEmptyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public XEmptyLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }
}
