package me.buryinmind.android.app.controller;

/**
 * Created by jasontujun on 2016/5/31.
 */
public interface ProgressListener<T> extends ResultListener<T> {
    void onProgress(T data, long completeSize, long totalSize);
}
