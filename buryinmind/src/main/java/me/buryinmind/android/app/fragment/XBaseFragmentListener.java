package me.buryinmind.android.app.fragment;

/**
 * Created by jasontujun on 2016/6/16.
 */
public abstract class XBaseFragmentListener implements XFragmentListener {
    @Override
    public void onLoading(boolean show) {
    }

    @Override
    public void onFinish(boolean result, Object data) {
    }

    @Override
    public void onEnter() {
    }

    @Override
    public void onExit() {
    }

    @Override
    public void onRefresh(int refreshEvent, Object data) {

    }
}
