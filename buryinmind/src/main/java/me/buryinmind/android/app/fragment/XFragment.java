package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.CallSuper;

import me.buryinmind.android.app.uicontrol.XAsyncRecycler;

/**
 * XFragment添加了配合Fragment声明周期的回调监听接口；
 * 添加了针对AsyncTask管理的方法{@link #putAsyncTask(AsyncTask)}
 * 和{@link #removeAsyncTask(AsyncTask)}；
 * 添加了Fragment对返回键的响应处理{@link #onBackHandle()}，
 * 需要{@link me.buryinmind.android.app.activity.XActivity}的支持。
 * Created by jasontujun on 2016/6/16.
 */
public class XFragment extends Fragment {

    private XFragmentListener mListener;
    private XAsyncRecycler mRecycler;

    @CallSuper
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRecycler = new XAsyncRecycler();
    }


    @CallSuper
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mListener != null) {
            mListener.onEnter();
        }
    }

    @CallSuper
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mListener != null) {
            mListener.onExit();
        }
        // 暂停并回收所有AsyncTask
        mRecycler.recycle();
    }

    public boolean onBackHandle() {
        return false;
    }

    protected boolean putAsyncTask(AsyncTask task) {
        return mRecycler.put(task);
    }

    protected boolean removeAsyncTask(AsyncTask task) {
        return mRecycler.remove(task);
    }

    protected void notifyLoading(boolean loading) {
        if (mListener != null)
            mListener.onLoading(loading);
    }

    protected void notifyFinish(boolean result, Object data) {
        if (mListener != null) {
            mListener.onFinish(result, data);
        }
    }

    protected void notifyRefresh(int refreshEvent, Object data) {
        if (mListener != null) {
            mListener.onRefresh(refreshEvent, data);
        }
    }

    public void setListener(XFragmentListener listener) {
        mListener = listener;
    }
}
