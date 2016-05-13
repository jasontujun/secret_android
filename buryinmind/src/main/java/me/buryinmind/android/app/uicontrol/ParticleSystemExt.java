package me.buryinmind.android.app.uicontrol;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;

import com.plattysoft.leonids.ParticleSystem;

import java.lang.reflect.Field;

/**
 * Created by jasontujun on 2016/5/12.
 */
public class ParticleSystemExt extends ParticleSystem {
    private Field pl;
    private Field exmin;
    private Field exmax;
    private Field eymin;
    private Field eymax;

    public ParticleSystemExt(Activity a, int maxParticles, int drawableRedId, long timeToLive) {
        super(a, maxParticles, drawableRedId, timeToLive);
        init();
    }

    public ParticleSystemExt(Activity a, int maxParticles, int drawableRedId, long timeToLive, int parentViewId) {
        super(a, maxParticles, drawableRedId, timeToLive, parentViewId);
        init();
    }

    public ParticleSystemExt(Activity a, int maxParticles, Drawable drawable, long timeToLive) {
        super(a, maxParticles, drawable, timeToLive);
        init();
    }

    public ParticleSystemExt(Activity a, int maxParticles, Drawable drawable, long timeToLive, int parentViewId) {
        super(a, maxParticles, drawable, timeToLive, parentViewId);
        init();
    }

    public ParticleSystemExt(Activity a, int maxParticles, Bitmap bitmap, long timeToLive) {
        super(a, maxParticles, bitmap, timeToLive);
        init();
    }

    public ParticleSystemExt(Activity a, int maxParticles, Bitmap bitmap, long timeToLive, int parentViewId) {
        super(a, maxParticles, bitmap, timeToLive, parentViewId);
        init();
    }

    public ParticleSystemExt(Activity a, int maxParticles, AnimationDrawable animation, long timeToLive) {
        super(a, maxParticles, animation, timeToLive);
        init();
    }

    public ParticleSystemExt(Activity a, int maxParticles, AnimationDrawable animation, long timeToLive, int parentViewId) {
        super(a, maxParticles, animation, timeToLive, parentViewId);
        init();
    }

    private void init() {
        try {
            pl = ParticleSystem.class.getDeclaredField("mParentLocation");
            pl.setAccessible(true);
            exmin = ParticleSystem.class.getDeclaredField("mEmiterXMin");
            exmin.setAccessible(true);
            exmax = ParticleSystem.class.getDeclaredField("mEmiterXMax");
            exmax.setAccessible(true);
            eymin = ParticleSystem.class.getDeclaredField("mEmiterYMin");
            eymin.setAccessible(true);
            eymax = ParticleSystem.class.getDeclaredField("mEmiterYMax");
            eymax.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * 竖直方向上产生
     *
     * @param emitterX
     */
    public void updateEmitVerticalLine(int emitterX, int emitterMinY, int emitterMaxY) {
        configureEmiter(emitterX, emitterMinY, emitterMaxY);
    }

    private void configureEmiter(int emitterX, int emitterMinY, int emitterMaxY) {
        // We configure the emiter based on the window location to fix the offset of action bar if present
        try {
            Object plobj = pl.get(this);
            int[] ploint = (int[]) plobj;
            int x = emitterX - ploint[0];
            exmin.setInt(this, x);
            exmax.setInt(this, x);
            eymin.setInt(this, emitterMinY);
            eymax.setInt(this, emitterMaxY);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
