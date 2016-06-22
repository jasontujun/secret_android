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

import com.tj.xengine.android.utils.XLog;


/**
 * 滑动时带粒子效果的FrameLayout.
 */
public class XParticleLayout extends FrameLayout {

    public interface Listener {
        void onStart();

        void onEnd();

        void onCancelled();
    }

    private static final int COUNT_OF_PARTICLE_BITMAP = 300;
    private static final int TIME_TO_LIVE = 1000;
    private static final int TIME_TO_FADE_OUT = 200;
    public static final float DEFAULT_START_RATIO = 0.1f;
    public static final float DEFAULT_END_RATIO = 0.5f;
    public enum Orientation {
        FROM_LEFT,
        FROM_RIGHT,
        FROM_TOP,
        FROM_BOTTOM
    }

    private ViewGroup backLayout;

    private boolean isEnable = true;
    private boolean isSwipe = false;
    private boolean isHide = false;
    private Orientation mOrientation;
    private float mStartRatio;// 从滑动方向计算，触摸开始位置的百分比
    private float mEndRatio;// 从滑动方向计算，触摸结束位置的百分比
    private int clipLength;
    int[] contentLocation;
    private Rect contentRect;
    private int mParticleBitmap;
    private Listener mListener;
    private XParticleSystem particleSystem;

    public XParticleLayout(Context context) {
        this(context, null);
    }

    public XParticleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XParticleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        contentRect = new Rect();
        contentLocation = new int[2];
        clipLength = 0;
        mStartRatio = DEFAULT_START_RATIO;
        mEndRatio = DEFAULT_END_RATIO;
        mOrientation = Orientation.FROM_RIGHT;
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
    protected void dispatchDraw(Canvas canvas) {
        if (isHide) {
            canvas.clipRect(0, 0, 0, 0);
        } else {
            switch (mOrientation) {
                case FROM_LEFT:
                    canvas.clipRect(clipLength, 0, getWidth(), getHeight());
                    break;
                case FROM_RIGHT:
                    canvas.clipRect(0, 0, getWidth() - clipLength, getHeight());
                    break;
                case FROM_TOP:
                    canvas.clipRect(0, clipLength, getWidth(), getHeight());
                    break;
                case FROM_BOTTOM:
                    canvas.clipRect(0, 0, getWidth(), getHeight() - clipLength);
                    break;
            }
        }
        super.dispatchDraw(canvas);
    }

    private int getParticleGravity() {
        switch (mOrientation) {
            case FROM_LEFT:
                return Gravity.LEFT;
            case FROM_RIGHT:
                return Gravity.RIGHT;
            case FROM_TOP:
                return Gravity.TOP;
            case FROM_BOTTOM:
                return Gravity.BOTTOM;
        }
        return Gravity.RIGHT;
    }

    private boolean checkStart(MotionEvent event) {
        switch (mOrientation) {
            case FROM_LEFT:
                return event.getX() < contentRect.width() * mStartRatio;
            case FROM_RIGHT:
                return event.getX() > contentRect.width() * (1 - mStartRatio);
            case FROM_TOP:
                return event.getY() < contentRect.height() * mStartRatio;
            case FROM_BOTTOM:
                return event.getY() > contentRect.height() * (1 - mStartRatio);
        }
        return false;
    }

    private boolean checkEnd(MotionEvent event) {
        switch (mOrientation) {
            case FROM_LEFT:
                return event.getX() > contentRect.width() * mEndRatio;
            case FROM_RIGHT:
                return event.getX() < contentRect.width() * (1 - mEndRatio);
            case FROM_TOP:
                return event.getY() > contentRect.height() * mEndRatio;
            case FROM_BOTTOM:
                return event.getY() < contentRect.height() * (1 - mEndRatio);
        }
        return false;
    }

    private int calClipLength(MotionEvent event) {
        switch (mOrientation) {
            case FROM_LEFT:
                return (int) (event.getX());
            case FROM_RIGHT:
                return (int) (contentRect.width() - event.getX());
            case FROM_TOP:
                return (int) event.getY();
            case FROM_BOTTOM:
                return (int) (contentRect.height() - event.getY());
        }
        return 0;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        XLog.d("ParticleLayout", "onInterceptTouchEvent.e=" + event);
        if (!isEnable || isHide) {
            return false;
        }
        if (checkStart(event)) {
            isSwipe = true;
        }
        return isSwipe;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        XLog.d("ParticleLayout", "onTouchEvent.e=" + event);
        if (!isEnable || isHide) {
            // 已经隐藏的item，不再触发滑动粒子效果
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isSwipe) {
                    if (mParticleBitmap > 0) {
                        particleSystem = new XParticleSystem((Activity) getContext(),
                                COUNT_OF_PARTICLE_BITMAP, mParticleBitmap, TIME_TO_LIVE);
                        particleSystem.setAcceleration(0.00013f, 90)
                                .setSpeedByComponentsRange(0f, 0.3f, 0.05f, 0.3f)
                                .setFadeOut(TIME_TO_FADE_OUT, new AccelerateInterpolator())
                                .emitWithGravity(backLayout, getParticleGravity(), COUNT_OF_PARTICLE_BITMAP);
                    }
                    if (mListener != null) {
                        mListener.onStart();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isSwipe) {
                    clipLength = calClipLength(event);
                    if (clipLength > 0) {
                        requestLayout();
                        int statusBarHeight = getStatusBarHeight();
                        if (particleSystem != null) {
                            switch (mOrientation) {
                                case FROM_LEFT:
                                    particleSystem.updateEmitVerticalLine(contentRect.left + clipLength,
                                            contentRect.top - statusBarHeight, contentRect.bottom - statusBarHeight);
                                    break;
                                case FROM_RIGHT:
                                    particleSystem.updateEmitVerticalLine(contentRect.right - clipLength,
                                            contentRect.top - statusBarHeight, contentRect.bottom - statusBarHeight);
                                    break;
                                case FROM_TOP:
                                    particleSystem.updateEmitHorizontalLine(contentRect.top + clipLength,
                                            contentRect.left, contentRect.right);
                                    break;
                                case FROM_BOTTOM:
                                    particleSystem.updateEmitHorizontalLine(contentRect.bottom - clipLength,
                                            contentRect.left, contentRect.right);
                                    break;
                            }
                        }
                        getParent().requestDisallowInterceptTouchEvent(true);
                    } else {
                        if (particleSystem != null) {
                            particleSystem.stopEmitting();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                clipLength = 0;
                if (isSwipe) {
                    if (particleSystem != null) {
                        particleSystem.stopEmitting();
                        particleSystem = null;
                    }
                    invalidate();
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (checkEnd(event)) {
                        isHide = true;
                        // 滑动超过阀值，有效结束
                        if (mListener != null) {
                            mListener.onEnd();
                        }
                    } else {
                        // 滑动未超过阀值，无效结束
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

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    public boolean isEnable() {
        return isEnable;
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

    public void setOrientation(Orientation orientation) {
        mOrientation = orientation;
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
