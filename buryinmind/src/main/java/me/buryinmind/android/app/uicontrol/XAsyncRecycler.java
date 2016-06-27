package me.buryinmind.android.app.uicontrol;

import android.os.AsyncTask;
import android.support.annotation.MainThread;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontujun on 2016/6/25.
 */
public class XAsyncRecycler {

    private List<AsyncTask> mTasks;

    public XAsyncRecycler() {
        mTasks = new ArrayList<>();
    }

    @MainThread
    public void recycle() {
        for (AsyncTask task : mTasks) {
            if (task != null && task.getStatus() != AsyncTask.Status.FINISHED)
                task.cancel(true);
        }
        mTasks.clear();
    }

    @MainThread
    public boolean put(AsyncTask task) {
        if (!mTasks.contains(task)) {
            mTasks.add(task);
            return true;
        }
        return false;
    }

    @MainThread
    public boolean remove(AsyncTask task) {
        return mTasks.remove(task);
    }
}
