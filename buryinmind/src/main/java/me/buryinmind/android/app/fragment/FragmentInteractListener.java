package me.buryinmind.android.app.fragment;

import java.io.Serializable;

/**
 * Created by jasontujun on 2016/4/20.
 */
public interface FragmentInteractListener {

    void onLoading(boolean show);

    void onBack();

    void onFinish(boolean result, Object data);
}
