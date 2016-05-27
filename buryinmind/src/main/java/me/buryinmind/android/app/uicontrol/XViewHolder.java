package me.buryinmind.android.app.uicontrol;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

/**
 * Created by jasontujun on 2016/5/19.
 */
public class XViewHolder extends RecyclerView.ViewHolder {

    private final SparseArray<View> mViews;
    private Object mData;

    public XViewHolder(View itemView) {
        super(itemView);
        this.mViews = new SparseArray<View>();
    }

    public View getView(int viewId) {
        View view = mViews.get(viewId);
        if (view == null) {
            view = itemView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        return view;
    }

    public <T extends View> T getView(int viewId, Class<T> clazz) {
        View view = getView(viewId);
        return clazz.cast(view);
    }

    public void bindData(Object data) {
        mData = data;
    }

    public Object getData() {
        return mData;
    }

}
