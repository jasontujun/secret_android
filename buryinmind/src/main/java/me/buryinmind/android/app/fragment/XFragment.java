package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.CallSuper;

/**
 * Created by jasontujun on 2016/6/16.
 */
public class XFragment extends Fragment {

    private XFragmentListener mListener;

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
