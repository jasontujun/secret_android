package me.buryinmind.android.app.uicontrol;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.daimajia.swipe.SwipeLayout;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by jasontujun on 2016/6/11.
 */
public class XSwipeLayout extends SwipeLayout {
    private static final int DRAG_LEFT = 1;
    private static final int DRAG_RIGHT = 2;
    private static final int DRAG_TOP = 4;
    private static final int DRAG_BOTTOM = 8;

    private Field currentDragEdge;
    private Field layoutListener;
    private Field swipeListener;
    private Method computeSurfaceLayout;
    private Method computeBottomLayout;
    private Method safeBottomView;
    private Method updateBottomViews;

    public XSwipeLayout(Context context) {
        super(context);
        init(context, null);
    }

    public XSwipeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public XSwipeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        try {
            currentDragEdge = SwipeLayout.class.getDeclaredField("mCurrentDragEdge");
            currentDragEdge.setAccessible(true);
            layoutListener = SwipeLayout.class.getDeclaredField("mOnLayoutListeners");
            layoutListener.setAccessible(true);
            swipeListener = SwipeLayout.class.getDeclaredField("mSwipeListeners");
            swipeListener.setAccessible(true);
            computeSurfaceLayout = SwipeLayout.class.getDeclaredMethod
                    ("computeSurfaceLayoutArea", boolean.class);
            computeSurfaceLayout.setAccessible(true);
            computeBottomLayout = SwipeLayout.class.getDeclaredMethod
                    ("computeBottomLayoutAreaViaSurface", ShowMode.class, Rect.class);
            computeBottomLayout.setAccessible(true);
            safeBottomView = SwipeLayout.class.getDeclaredMethod("safeBottomView");
            safeBottomView.setAccessible(true);
            updateBottomViews = SwipeLayout.class.getDeclaredMethod("updateBottomViews");
            updateBottomViews.setAccessible(true);
            if (attrs != null) {
                TypedArray a = context.obtainStyledAttributes(attrs, com.daimajia.swipe.R.styleable.SwipeLayout);
                int dragEdgeChoices = a.getInt(com.daimajia.swipe.R.styleable.SwipeLayout_drag_edge, 2);
                if ((dragEdgeChoices & DRAG_LEFT) == DRAG_LEFT) {
                    currentDragEdge.set(this, DragEdge.Left);
                } else if ((dragEdgeChoices & DRAG_TOP) == DRAG_TOP) {
                    currentDragEdge.set(this, DragEdge.Top);
                } else if ((dragEdgeChoices & DRAG_RIGHT) == DRAG_RIGHT) {
                    currentDragEdge.set(this, DragEdge.Right);
                } else if ((dragEdgeChoices & DRAG_BOTTOM) == DRAG_BOTTOM) {
                    currentDragEdge.set(this, DragEdge.Bottom);
                }
                a.recycle();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        try {
            if (getOpenStatus() == Status.Open) {
                Rect rect = (Rect) computeSurfaceLayout.invoke(this, true);
                View surface = getSurfaceView(), bottom = getCurrentBottomView();
                if(surface != null){
                    surface.layout(rect.left, rect.top, rect.right, rect.bottom);
                }
                if(bottom != null){
                    if (getShowMode() == ShowMode.PullOut) {
                        rect = (Rect) computeBottomLayout.invoke(this, ShowMode.PullOut, rect);
                        bottom.layout(rect.left, rect.top, rect.right, rect.bottom);
                    }
                    if (getShowMode() == ShowMode.LayDown) {
                        rect = (Rect) computeBottomLayout.invoke(this, ShowMode.LayDown, rect);
                        bottom.layout(rect.left, rect.top, rect.right, rect.bottom);
                    }
                }
                safeBottomView.invoke(this);
            } else {
                updateBottomViews.invoke(this);
            }
            List<OnLayout> layoutListeners = (List<OnLayout>) layoutListener.get(this);
            if (layoutListeners != null) {
                for (int i = 0; i < layoutListeners.size(); i++) {
                    layoutListeners.get(i).onLayout(this);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addSwipeListener(SwipeListener l) {
        try {
            List<SwipeListener> swipeListeners = (List<SwipeListener>) swipeListener.get(this);
            if (swipeListeners != null) {
                if (!swipeListeners.contains(l)) {
                    swipeListeners.add(l);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
