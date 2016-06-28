package me.buryinmind.android.app.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.utils.XStringUtil;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.fragment.MemoryGiftFragment;
import me.buryinmind.android.app.fragment.PostMemoryFragment;
import me.buryinmind.android.app.fragment.SearchFriendsFragment;
import me.buryinmind.android.app.fragment.XBaseFragmentListener;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ViewUtil;


/**
 * Created by jasontujun on 2016/6/24.
 */
public class MemoryGiftActivity extends XActivity {

    private static final String TAG = MemoryGiftActivity.class.getSimpleName();

    private MemoryGiftFragment mGiftFragment;
    private SearchFriendsFragment mFriendsFragment;
    private PostMemoryFragment mPostFragment;

    private View mProgressView;
    private View mContentLayout;
    private View mToolBarStubLayout;
    private View mToolBarLayout;
    private TextView mToolBarTitle;
    private Toolbar mToolBar;
    private MenuItem mPostBtn;
    private MenuItem mDoneBtn;

    private Memory mMemory;
    private boolean mShowToolBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        XLog.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_gift);

        Intent intent = getIntent();
        String memoryId = intent.getStringExtra("mid");
        if (XStringUtil.isEmpty(memoryId)) {
            // TODO error
            return;
        }
        XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
        mMemory = source.getById(memoryId);
        if (mMemory == null) {
            // TODO error
            return;
        }

        mProgressView = findViewById(R.id.loading_progress);
        mContentLayout = findViewById(R.id.content_layout);
        mToolBarStubLayout = findViewById(R.id.toolbar_stub_layout);
        mToolBarLayout = findViewById(R.id.toolbar_layout);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBarTitle = (TextView) findViewById(R.id.toolbar_title);

        // init toolbar
        mShowToolBar = true;
        mToolBar.setTitle("");
        mToolBarTitle.setText(mMemory.name);
        setSupportActionBar(mToolBar);

        // 为了先执行onCreateOptionsMenu(),所以延迟添加Fragment
        mContentLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mGiftFragment == null) {
                    mGiftFragment = (MemoryGiftFragment) createSecretsFragment();
                }
                getFragmentManager().beginTransaction()
                        .add(R.id.content_layout, mGiftFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }
        }, 50);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_memory, menu);
        MenuItem mAddBtn = menu.getItem(0);
        mPostBtn = menu.getItem(1);
        mDoneBtn = menu.getItem(2);
        mAddBtn.setVisible(false);
        mPostBtn.setVisible(false);
        mDoneBtn.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case  R.id.action_post:
                goToNext(SearchFriendsFragment.class, null);
                return true;
            case R.id.action_done:
                mPostFragment.confirm();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Fragment getCurrentFragment() {
        return getFragmentManager().findFragmentById(R.id.content_layout);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        XLog.d(TAG, "onConfigurationChanged()");
    }

    private void showProgress(boolean show) {
        ViewUtil.animateFadeInOut(mContentLayout, show);
        ViewUtil.animateFadeInOut(mProgressView, !show);
    }

    private void refreshMenu() {
        Fragment current = getCurrentFragment();
        if (current == null) {
            return;
        }
        if (current instanceof MemoryGiftFragment) {
            if (mMemory.secrets.size() > 0) {
                mPostBtn.setVisible(true);
            } else {
                mPostBtn.setVisible(false);
            }
            mDoneBtn.setVisible(false);
        } else if (current instanceof SearchFriendsFragment) {
            mPostBtn.setVisible(false);
            mDoneBtn.setVisible(false);
        } else if (current instanceof PostMemoryFragment) {
            mPostBtn.setVisible(false);
            mDoneBtn.setVisible(true);
        }
    }

    private void goToNext(Class<?> clazz, Object data) {
        Fragment current = getCurrentFragment();
        if (current == null)
            return;
        if (current instanceof MemoryGiftFragment &&
                clazz == SearchFriendsFragment.class) {
            if (mFriendsFragment == null) {
                mFriendsFragment = createFriendsFragment();
            }
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_layout, mFriendsFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        } else if (current instanceof SearchFriendsFragment &&
                clazz == PostMemoryFragment.class) {
            User user = (User) data;
            if (mPostFragment == null) {
                mPostFragment = createPostFragment(user);
            } else {
                mPostFragment.getArguments().putSerializable(PostMemoryFragment.KEY_RECEIVER, user);
            }
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_layout, mPostFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }


    private Fragment createSecretsFragment() {
        final MemoryGiftFragment fragment = new MemoryGiftFragment();
        fragment.setListener(new XBaseFragmentListener() {
            @Override
            public void onEnter() {
                refreshMenu();
                mToolBarStubLayout.setVisibility(View.GONE);
                mToolBarTitle.setText(mMemory.name);
            }

            @Override
            public void onExit() {
                mToolBarStubLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoading(boolean show) {
                showProgress(show);
            }

            @Override
            public void onRefresh(int refreshEvent, Object data) {
                switch (refreshEvent) {
                    case MemoryGiftFragment.REFRESH_UP:
                        showToolBar();
                        break;
                    case MemoryGiftFragment.REFRESH_DOWN:
                        hideToolBar();
                        break;
                    case MemoryGiftFragment.REFRESH_DATA:
                        refreshMenu();
                        break;
                }
            }
        });
        Bundle arguments = new Bundle();
        arguments.putString(MemoryGiftFragment.KEY_MID, mMemory.mid);
        fragment.setArguments(arguments);
        return fragment;
    }

    private SearchFriendsFragment createFriendsFragment() {
        SearchFriendsFragment fragment = new SearchFriendsFragment();
        fragment.setListener(
                new XBaseFragmentListener() {
                    @Override
                    public void onEnter() {
                        refreshMenu();
                    }

                    @Override
                    public void onFinish(boolean result, Object data) {
                        if (result) {
                            goToNext(PostMemoryFragment.class, data);
                        }
                    }
                });
        Bundle arguments = new Bundle();
        arguments.putString(SearchFriendsFragment.KEY_MID, mMemory.mid);
        fragment.setArguments(arguments);
        return fragment;
    }

    private PostMemoryFragment createPostFragment(User user) {
        PostMemoryFragment fragment = new PostMemoryFragment();
        fragment.setListener(new XBaseFragmentListener() {

            @Override
            public void onEnter() {
                refreshMenu();
            }

            @Override
            public void onLoading(boolean show) {
                showProgress(show);
            }

            @Override
            public void onFinish(boolean result, Object data) {
                if (result) {
                    // 发送成功,重新进入Memory详情界面
                    Intent intent = new Intent(MemoryGiftActivity.this, MemoryGiftActivity.class);
                    intent.putExtra("mid", mMemory.mid);
                    startActivity(intent);
                    finish();
                }
            }
        });
        Bundle arguments = new Bundle();
        arguments.putString(PostMemoryFragment.KEY_MID, mMemory.mid);
        arguments.putSerializable(PostMemoryFragment.KEY_RECEIVER, user);
        fragment.setArguments(arguments);
        return fragment;
    }



    public void hideToolBar() {
        if (mShowToolBar) {
            mShowToolBar = false;
            mToolBarLayout.animate().translationY(-mToolBarLayout.getHeight())
                    .setInterpolator(new AccelerateDecelerateInterpolator());
        }
    }

    public void showToolBar() {
        if (!mShowToolBar) {
            mShowToolBar = true;
            mToolBarLayout.animate().translationY(0)
                    .setInterpolator(new AccelerateDecelerateInterpolator());
        }
    }
}
