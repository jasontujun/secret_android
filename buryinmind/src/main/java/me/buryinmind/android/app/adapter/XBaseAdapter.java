package me.buryinmind.android.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontujun on 2016/6/6.
 */
public abstract class XBaseAdapter<T> extends BaseAdapter {

    private int mLayoutId;
    private final List<T> mValues;

    public XBaseAdapter(int layoutId) {
        this(layoutId, null);
    }

    public XBaseAdapter(int layoutId, List<T> items) {
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
        notifyDataSetChanged();
    }

    public void addData(List<T> data) {
        int position = mValues.size();
        mValues.addAll(data);
        notifyDataSetChanged();
    }

    public void deleteData(T data) {
        int pos = mValues.indexOf(data);
        if (pos == -1) {
            return;
        }
        mValues.remove(data);
        notifyDataSetChanged();
    }

    public void deleteData(int pos) {
        if (0 <= pos && pos < mValues.size()) {
            mValues.remove(pos);
            notifyDataSetChanged();
        }
    }

    public void refreshData(T data, Object payload) {
        int pos = mValues.indexOf(data);
        if (pos == -1) {
            return;
        }
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return mValues.get(position);
    }

    @Override
    public int getCount() {
        return mValues.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        XViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
            holder = new XViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (XViewHolder) convertView.getTag();
        }
        onBindViewHolder(holder, position);
        return convertView;
    }

    public abstract void onBindViewHolder(XViewHolder holder, int position);
}
