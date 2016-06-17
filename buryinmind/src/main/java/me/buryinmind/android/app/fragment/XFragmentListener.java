package me.buryinmind.android.app.fragment;

/**
 * Fragment对Activity的回调接口定义。
 * Created by jasontujun on 2016/4/20.
 */
public interface XFragmentListener {

    void onLoading(boolean loading);

    void onFinish(boolean result, Object data);

    /**
     * 进入Fragment的回调。
     * 可以在此处添加父Activity的相关处理。
     */
    void onEnter();

    /**
     * 离开Fragment的回调。
     * 可以在此处添加父Activity的相关处理。
     */
    void onExit();

    /**
     * 自定义的刷新回调(通知activity刷新)
     * @param refreshEvent 刷新事件
     * @param data 刷新的数据
     */
    void onRefresh(int refreshEvent, Object data);
}
