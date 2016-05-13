package me.buryinmind.android.app.uicontrol;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;


/**
 * 滑动时带粒子效果的FrameLayout.
 */
public class ParticleLayout extends FrameLayout {

    public interface Listener {
        void onStart();

        void onEnd();

        void onCancelled();
    }

    private static final int COUNT_OF_PARTICLE_BITMAP = 300;
    private static final int TIME_TO_LIVE = 1000;
    private static final int TIME_TO_FADE_OUT = 200;
    public static final float DEFAULT_START_RATIO = 0.8f;
    public static final float DEFAULT_END_RATIO = 0.5f;

    private ViewGroup backLayout;

    private boolean isSwipe = false;
    private boolean isHide = false;
    private float mStartRatio;
    private float mEndRatio;
    private float startX;
    private int clipWidth = 0;
    int[] contentLocation;
    private Rect contentRect;
    private int mParticleBitmap;
    private Listener mListener;
    private ParticleSystemExt particleSystem;

    public ParticleLayout(Context context) {
        this(context, null);
    }

    public ParticleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ParticleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        contentRect = new Rect();
        contentLocation = new int[2];
        mStartRatio = DEFAULT_START_RATIO;
        mEndRatio = DEFAULT_END_RATIO;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (getChildCount() != 1) {
            throw new IllegalArgumentException("the count of child view must be one !");
        }

        backLayout = (ViewGroup) getChildAt(0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        backLayout.getLocationInWindow(contentLocation);
        contentRect.set(contentLocation[0], contentLocation[1],
                contentLocation[0] + backLayout.getMeasuredWidth(),
                contentLocation[1] + backLayout.getMeasuredHeight());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isHide) {
            canvas.clipRect(0, 0, 0, getHeight());
        } else {
            canvas.clipRect(0, 0, getWidth() - clipWidth, getHeight());
        }
        super.dispatchDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isHide) {
            // 已经隐藏的item，不再触发滑动粒子效果
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() > contentRect.width() * mStartRatio) {
                    isSwipe = true;
                    startX = contentRect.width();
                    if (mParticleBitmap > 0) {
                        particleSystem = new ParticleSystemExt((Activity) getContext(),
                                COUNT_OF_PARTICLE_BITMAP, mParticleBitmap, TIME_TO_LIVE);
                        particleSystem.setAcceleration(0.00013f, 90)
                                .setSpeedByComponentsRange(0f, 0.3f, 0.05f, 0.3f)
                                .setFadeOut(TIME_TO_FADE_OUT, new AccelerateInterpolator())
                                .emitWithGravity(backLayout, Gravity.RIGHT, COUNT_OF_PARTICLE_BITMAP);
                    }
                    if (mListener != null) {
                        mListener.onStart();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                clipWidth = (int) (startX - event.getX());
                if (isSwipe && clipWidth > 0) {
                    requestLayout();
                    int statusBarHeight = getStatusBarHeight();
                    if (particleSystem != null) {
                        particleSystem.updateEmitVerticalLine(contentRect.right - clipWidth,
                                contentRect.top - statusBarHeight, contentRect.bottom - statusBarHeight);
                    }
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else {
                    if (particleSystem != null) {
                        particleSystem.stopEmitting();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                startX = 0;
                clipWidth = 0;
                if (isSwipe) {
                    if (particleSystem != null) {
                        particleSystem.stopEmitting();
                        particleSystem = null;
                    }
                    invalidate();
                    getParent().requestDisallowInterceptTouchEvent(false);

                    if (event.getX() < getWidth() * mEndRatio) {
                        isHide = true;
                        // 滑动超过1/2，有效结束
                        if (mListener != null) {
                            mListener.onEnd();
                        }
                    } else {
                        // 滑动未超过1/2，无效结束
                        if (mListener != null) {
                            mListener.onCancelled();
                        }
                    }
                }
                isSwipe = false;
                break;
        }

        return isSwipe || super.onTouchEvent(event);

    }

    public void hide() {
        isHide = true;
        invalidate();
    }

    public void reset() {
        isHide = false;
        invalidate();
    }

    public void setRatio(int startRatio, int endRatio) {
        mStartRatio = startRatio;
        mEndRatio = endRatio;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setParticle(int resId) {
        mParticleBitmap = resId;
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

}
