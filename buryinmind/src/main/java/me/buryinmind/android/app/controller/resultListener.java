package me.buryinmind.android.app.controller;

/**
 * 对结果的回调接口。
 * Created by jasontujun on 2016/5/25.
 */
public interface ResultListener<T> {

    void onResult(boolean result, T data);
}
