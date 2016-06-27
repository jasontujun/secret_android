package me.buryinmind.android.app.activity;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import me.buryinmind.android.app.fragment.XFragment;
import me.buryinmind.android.app.uicontrol.XAsyncRecycler;

/**
 * XActivity主要封装了对fragment的管理。
 * 主要是使用{@link #getFragmentManager()}，而不是{@link #getSupportFragmentManager()}，
 * 如果要复用XActivity的Fragment管理功能，请在使用时注意，务必使用{@link #getFragmentManager()}，
 * 且正确覆盖{@link #getCurrentFragment()}方法。
 * 如果要自定义处理返回操作，请覆盖{@link #onBackHandle()}。
 * Created by jasontujun on 2016/6/25.
 */
public class XActivity extends AppCompatActivity {
    private XAsyncRecycler mRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRecycler = new XAsyncRecycler();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecycler.recycle();
    }

    /**
     * 获取FragmentManager当前的Fragment引用。
     * 比如可以通过getFragmentManager().findFragmentById(id)的方式获取。
     * @return 如果存在则返回Fragment引用，否则返回null。
     */
    protected Fragment getCurrentFragment() {
        return null;
    }

    /**
     * 自定义的back键处理。
     * 此方法在XFragment管理后执行,如果XFragment没有响应back操作，
     * 且没有Fragment被弹出，则调用此方法。
     * 如果此方法内响应了back键，请返回true；否则返回false，执行默认的处理(一般为退出Activity)
     * @return
     */
    protected boolean onBackHandle() {
        return false;
    }

    @Override
    public final void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            Fragment fragment = getCurrentFragment();
            if (fragment != null && fragment instanceof XFragment) {
                // 优先调用XFragment内部的返回逻辑，如果返回true，
                // 则表示返回操作已在XFragment内部完全响应，无需后续处理。
                if (((XFragment) fragment).onBackHandle()) {
                    return;
                }
            }
            // 优先弹出fragment
            getFragmentManager().popBackStack();
        } else {
            if (onBackHandle()) {
                return;
            }
            super.onBackPressed();
        }
    }

    protected boolean putAsyncTask(AsyncTask task) {
        return mRecycler.put(task);
    }

    protected boolean removeAsyncTask(AsyncTask task) {
        return mRecycler.remove(task);
    }
}
