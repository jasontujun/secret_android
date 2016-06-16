package me.buryinmind.android.app.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontujun on 2016/6/6.
 */
public abstract class XListAdapter<T> extends RecyclerView.Adapter<XViewHolder> {

    private int mLayoutId;
    private final List<T> mValues;

    public XListAdapter(int layoutId) {
        this(layoutId, null);
    }

    public XListAdapter(int layoutId, List<T> items) {
        mLayoutId = layoutId;
        mValues = items == null ? new ArrayList<T>() : new ArrayList<T>(items);
    }

    public List<T> getData() {
        return mValues;
    }

    public void setData(List<T> data) {
        mValues.clear();
        mValues.addAll(data);
        notifyDataSetChanged();
    }

    public void addData(T data) {
        int position = mValues.size();
        mValues.add(data);
        notifyItemInserted(position);
    }

    public void addData(List<T> data) {
        int position = mValues.size();
        mValues.addAll(data);
        notifyItemRangeInserted(position, data.size());
    }

    public void deleteData(T data) {
        int pos = mValues.indexOf(data);
        if (pos == -1) {
            return;
        }
        mValues.remove(data);
        notifyItemRemoved(pos);
    }

    public void deleteData(int pos) {
        if (0 <= pos && pos < mValues.size()) {
            mValues.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    public void refreshData(T data, Object payload) {
        int pos = mValues.indexOf(data);
        if (pos == -1) {
            return;
        }
        notifyItemChanged(pos, payload);
    }

    @Override
    public XViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(mLayoutId, parent, false);
        return new XViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }
}
