package me.buryinmind.android.app.fragment;

import java.io.Serializable;

/**
 * Created by jasontujun on 2016/4/20.
 */
public interface FragmentInteractListener extends Serializable {

    public static final String KEY = "listener";

    void onLoading();

    void onBack();

    void onFinish(boolean result, Object data);
}
