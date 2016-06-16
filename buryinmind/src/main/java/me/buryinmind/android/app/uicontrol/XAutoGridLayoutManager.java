package me.buryinmind.android.app.uicontrol;

import android.content.Context;
import android.graphics.Point;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.tj.xengine.android.utils.XLog;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import me.buryinmind.android.app.adapter.DescriptionAdapter;

/**
 * Created by jasontujun on 2016/6/14.
 */
public class XAutoGridLayoutManager extends GridLayoutManager {

    private static final String TAG = DescriptionAdapter.TAG;

    public XAutoGridLayoutManager(Context context, AttributeSet attrs,
                                  int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public XAutoGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
        init();
    }

    public XAutoGridLayoutManager(Context context, int spanCount,
                                  int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
        init();
    }

    private void init() {
        setSpanSizeLookup(new AutoSpanSizeLookup());
    }

    private class AutoSpanSizeLookup extends SpanSizeLookup {

        private Field mRecyclerViewField;
        private Map<Integer, Point> mCacheSpanSize;

        public AutoSpanSizeLookup() {
            setSpanIndexCacheEnabled(true);
            mCacheSpanSize = new HashMap<Integer, Point>();
            try {
                mRecyclerViewField = RecyclerView.LayoutManager.class.getDeclaredField("mRecyclerView");
                mRecyclerViewField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void invalidateSpanIndexCache() {
            XLog.d(TAG, "invalidateSpanIndexCache()!!");
            super.invalidateSpanIndexCache();
            mCacheSpanSize.clear();
        }

        @Override
        public int getSpanSize(int i) {
            Point childSize = mCacheSpanSize.get(i);
            if (childSize == null) {
                View child = findViewByPosition(i);
                if (child == null) {
                    XLog.d(TAG, "child index=" + i + ", miss cache! createViewHolder()");
                    // 先生成childView去计算大小
                    try {
                        RecyclerView recyclerView = (RecyclerView) mRecyclerViewField.get(XAutoGridLayoutManager.this);
                        RecyclerView.Adapter adapter = recyclerView.getAdapter();
                        final int type = adapter.getItemViewType(i);
                        RecyclerView.ViewHolder vh = adapter.createViewHolder(recyclerView, type);
                        adapter.bindViewHolder(vh, i);
                        child = vh.itemView;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    XLog.d(TAG, "child index=" + i + ", miss cache! findViewByPosition()@");
                }
                if (child != null) {
                    if (child.getWidth() != 0 && child.getHeight() != 0) {
                        childSize = new Point(child.getWidth(), child.getHeight());
                        XLog.d(TAG, "child index=" + i + ", no measure()@");
                    } else {
                        child.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        childSize = new Point(child.getMeasuredWidth(), child.getMeasuredHeight());
                        XLog.d(TAG, "child index=" + i + ", measure()@");
                    }
                    mCacheSpanSize.put(i, childSize);
                } else {
                    XLog.d(TAG, "child index=" + i + ", child == null");
                    return getSpanCount();
                }
            } else {
                XLog.d(TAG, "child index=" + i + ", hit cache!");
            }
            int spanCount;
            if (getOrientation() == VERTICAL) {
                spanCount = getSpanCount() * childSize.x / getWidth();
                XLog.d(TAG, "child index=" + i + ", child width="
                        + childSize.x + ", spanCount=" + spanCount);
            } else {
                spanCount = getSpanCount() * childSize.y / getHeight();
                XLog.d(TAG, "child index=" + i + ", child height="
                        + childSize.y + ", spanCount=" + spanCount);
            }
            return Math.min(getSpanCount(), spanCount);
        }
    }
}
