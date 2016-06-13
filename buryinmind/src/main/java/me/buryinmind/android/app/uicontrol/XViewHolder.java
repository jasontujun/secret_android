package me.buryinmind.android.app.uicontrol;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

import com.tj.xengine.core.utils.XStringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jasontujun on 2016/5/19.
 */
public class XViewHolder extends RecyclerView.ViewHolder {

    private final SparseArray<View> mViews;
    private Object mData;
    private Map<String, Object> mTag;

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

    public <T> void setTag(String key, T tag) {
        if (XStringUtil.isEmpty(key))
            return;
        if (mTag == null) {
            mTag = new HashMap<String, Object>();
        }
        mTag.put(key, tag);
    }

    public <T> T getTag(String key, Class<T> clazz) {
        if (mTag == null || XStringUtil.isEmpty(key))
            return null;
        return clazz.cast(mTag.get(key));
    }

}
