package me.buryinmind.android.app.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.android.network.http.XAsyncHttp;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.controller.ProgressListener;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.fragment.MemoryAddFragment;
import me.buryinmind.android.app.fragment.BirthdayFragment;
import me.buryinmind.android.app.fragment.UserDescriptionFragment;
import me.buryinmind.android.app.fragment.XBaseFragmentListener;
import me.buryinmind.android.app.fragment.TimelineFragment;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.FileUtils;
import me.buryinmind.android.app.util.ImageUtil;
import me.buryinmind.android.app.util.ViewUtil;

public class TimelineActivity extends XActivity {

    private static final String TAG = TimelineActivity.class.getSimpleName();

    private static final int HEADER_REQUEST_CODE = 8090;
    private static final int COVER_REQUEST_CODE = 8091;

    private TimelineFragment mTimelineFragment;
    private BirthdayFragment mBirthDayFragment;
    private UserDescriptionFragment mEditDesFragment;
    private MemoryAddFragment mAddFragment;

    private View mProgressView;
    private View mContentView;
    private Toolbar mToolBar;
    private AppBarLayout mAppBar;
    private CollapsingToolbarLayout mCollapsedLayout;
    private ImageView mProfileBackground;
    private ImageView mAccountHeadView;
    private TextView mAccountNameView;
    private TextView mAccountDesView;
    private View mAccountLine;

    private MenuItem mAddBtn;
    private MenuItem mLogoutBtn;
    private MenuItem mNextBtn;
    private MenuItem mDoneBtn;

    private boolean mCollapsed = false;
    private boolean mWaiting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        XLog.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        mProgressView = findViewById(R.id.loading_progress);
        mContentView = findViewById(R.id.content_layout);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
        mCollapsedLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mProfileBackground = (ImageView) findViewById(R.id.account_profile_bg);
        mAccountHeadView = (ImageView) findViewById(R.id.account_head_img);
        mAccountNameView = (TextView) findViewById(R.id.account_name_txt);
        mAccountDesView = (TextView) findViewById(R.id.account_des_txt);
        mAccountLine = findViewById(R.id.account_line);

        // init AppBarLayout expand and collapse
        setSupportActionBar(mToolBar);
        mAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset <= mToolBar.getHeight() - mCollapsedLayout.getHeight()) {
                    // 已经彻底折叠
                    XLog.d(TAG, "mAppBar already collapsed");
                    mCollapsed = true;
                } else {
                    mCollapsed = false;
                }
            }
        });

        // init user profile
        final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        mAccountNameView.setText(user.name);
        mAccountHeadView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWaiting) {
                    Toast.makeText(TimelineActivity.this, R.string.error_loading, Toast.LENGTH_SHORT).show();
                    return;
                }
                // 选择图片，并上传
                Intent target = FileUtils.createGetContentIntent();
                Intent intent = Intent.createChooser(target, getString(R.string.info_choose_head));
                try {
                    startActivityForResult(intent, HEADER_REQUEST_CODE);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(TimelineActivity.this, getString(R.string.error_select_picture),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        showProfilePicture(ApiUtil.getIdUrl(user.uid));

        // init description list
        mAccountDesView.setText(XStringUtil.list2String(user.descriptions, ", "));
        mAccountDesView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNext(UserDescriptionFragment.class);
            }
        });

        // 为了先执行onCreateOptionsMenu(),所以延迟添加Fragment
        mContentView.postDelayed(new Runnable() {
            @Override
            public void run() {
                // init first ui
                if (user.bornTime == null) {
                    // 用户没设置过生日，先设置生日，再进入时间线
                    goToNext(BirthdayFragment.class);
                } else {
                    // 进入时间线
                    goToNext(TimelineFragment.class);
                }
            }
        }, 50);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        XLog.d(TAG, "onCreateOptionsMenu()");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_timeline, menu);
        mAddBtn = menu.getItem(0);
        mLogoutBtn = menu.getItem(1);
        mNextBtn = menu.getItem(2);
        mDoneBtn = menu.getItem(3);
        mAddBtn.setVisible(false);
        mLogoutBtn.setVisible(false);
        mNextBtn.setVisible(false);
        mDoneBtn.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
        switch (item.getItemId()) {
            case android.R.id.home:
                if (current != null && !(current instanceof TimelineFragment)) {
                    onBackPressed();
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
            case R.id.action_add:
                goToNext(MemoryAddFragment.class);
                return true;
            case R.id.action_logout:
                logoutAccount();
                return true;
            case R.id.action_next:
                return true;
            case R.id.action_done:
                if (current != null) {
                    if (current instanceof BirthdayFragment) {
                        mBirthDayFragment.confirm();
                    } else if (current instanceof UserDescriptionFragment) {
                        mEditDesFragment.confirm();
                    } else if (current instanceof MemoryAddFragment) {
                        mAddFragment.confirm();
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.getCurrentFocus() != null){
            // 点击空白位置 隐藏软键盘
            ViewUtil.hideInputMethod(this);
        }
        return super .onTouchEvent(event);
    }

    @Override
    public boolean onBackHandle() {
        // 点击2次退出
        long currentTime = System.currentTimeMillis();
        GlobalSource source = (GlobalSource) XDefaultDataRepo
                .getInstance().getSource(MyApplication.SOURCE_GLOBAL);
        if (currentTime - source.getLastBackTime() <= GlobalSource.PRESS_BACK_INTERVAL) {
            return false;
        } else {
            source.setLastBackTime(currentTime);
            Toast.makeText(this, R.string.info_press_back_again, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case HEADER_REQUEST_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    // Get the Path of the selected file
                    final String path = FileUtils.getPath(this, data.getData());
                    if (XStringUtil.isEmpty(path)) {
                        Toast.makeText(this, getString(R.string.error_select_picture),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    mWaiting = true;
                    ImageUtil.compressAndUploadImage(this, ApiUtil.getHeadToken(),
                            path,
                            new ProgressListener<String>() {
                                @Override
                                public void onProgress(String path, long completeSize, long totalSize) {}

                                @Override
                                public void onResult(boolean result, String key) {
                                    if (result) {
                                        MyApplication.updateImageTimestamp();// 更新图片时间戳
                                        showProfilePicture(path);
                                    } else {
                                        Toast.makeText(TimelineActivity.this, R.string.error_upload_picture,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    mWaiting = false;
                                }
                            }
                    );
                }
                break;
            case COVER_REQUEST_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
                    if (current instanceof MemoryAddFragment) {
                        // Get the Path of the selected file
                        String path = FileUtils.getPath(this, data.getData());
                        mAddFragment.setCoverPath(path);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void refreshToolBar() {
        Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
        if (current == null) {
            return;
        }
        if (current instanceof TimelineFragment) {
            mToolBar.setNavigationIcon(R.drawable.logo_buryinmind_white_small);
            mToolBar.setTitle(R.string.app_name);
            mAddBtn.setVisible(true);
            mLogoutBtn.setVisible(true);
            mNextBtn.setVisible(false);
            mDoneBtn.setVisible(false);
        } else {
            mToolBar.setNavigationIcon(R.drawable.icon_arrow_back_white);
            mToolBar.setTitle(null);
            mAddBtn.setVisible(false);
            mLogoutBtn.setVisible(false);
            mNextBtn.setVisible(false);
            mDoneBtn.setVisible(true);
        }
    }

    private void showProgress(boolean show) {
        ViewUtil.animateFadeInOut(mContentView, show);
        ViewUtil.animateFadeInOut(mProgressView, !show);
    }

    private void showProfilePicture(String url) {
        XLog.d(TAG, "showProfilePicture(). head_url=" + url);
        Glide.with(TimelineActivity.this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)
                .error(R.drawable.profile_default)
                .into(mProfileBackground);
        Glide.with(TimelineActivity.this)
                .load(url)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.headicon_default)
                .into(mAccountHeadView);
    }

    private void logoutAccount() {
        MyApplication.getAsyncHttp().execute(
                ApiUtil.logoutUser(),
                new XAsyncHttp.Listener() {
                    @Override
                    public void onCancelled() {}

                    @Override
                    public void onNetworkError() {
                        Toast.makeText(TimelineActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        Toast.makeText(TimelineActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                        // 清空数据源
                        MyApplication.clearDataSource();
                        // 回到登录界面
                        final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
                        Intent intent = new Intent(TimelineActivity.this, LoginActivity.class);
                        intent.putExtra(LoginActivity.RE_LOGIN, user.name);
                        startActivity(intent);
                        finish();
                    }
                });
    }


    private void goToNext(Class<?> clazz) {
        final Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
        if (clazz == TimelineFragment.class) {
            if (mTimelineFragment == null) {
                mTimelineFragment = new TimelineFragment();
                mTimelineFragment.setListener(new XBaseFragmentListener() {
                    @Override
                    public void onEnter() {
                        refreshToolBar();
                        ViewUtil.animateExpand(mAccountLine, true);
                        ViewUtil.animateFadeInOut(mAccountDesView, false);
                        if (mTimelineFragment.isScrollToTop()) {
                            mAppBar.setExpanded(true, true);
                        }
//                        mTimelineFragment.needScrollToTop();
                    }

                    @Override
                    public void onLoading(boolean show) {
                        showProgress(show);
                    }

                    @Override
                    public void onRefresh(int refreshEvent, Object data) {
                        switch (refreshEvent) {
                            case TimelineFragment.REFRESH_EXPAND:
                                if (getFragmentManager().findFragmentById(R.id.content_layout)
                                        instanceof TimelineFragment) {
                                    mAppBar.setExpanded(true, true);
                                }
                                break;
                            case TimelineFragment.REFRESH_SET_BIRTHDAY:
                                goToNext(BirthdayFragment.class);
                                break;
                            case TimelineFragment.REFRESH_ADD_MEMORY:
                                goToNext(MemoryAddFragment.class);
                                break;
                        }
                    }
                });
            }
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_layout, mTimelineFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        } else if (clazz == BirthdayFragment.class) {
            if (mBirthDayFragment == null) {
                mBirthDayFragment = new BirthdayFragment();
            }
            mBirthDayFragment.setListener(new XBaseFragmentListener() {
                @Override
                public void onEnter() {
                    refreshToolBar();
                    if (!mCollapsed) {
                        mAppBar.setExpanded(false, true);
                    }
                    ViewUtil.animateExpand(mAccountLine, false);
                }

                @Override
                public void onLoading(boolean show) {
                    showProgress(show);
                }

                @Override
                public void onFinish(boolean result, Object data) {
                    showProgress(false);
                    if (result) {
                        if (current == null) {
                            goToNext(TimelineFragment.class);
                        } else {
                            // 模拟返回按钮
                            onBackPressed();
                        }
                    }
                }
            });
            if (current == null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_layout, mBirthDayFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            } else {
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_layout, mBirthDayFragment)
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }
        } else if (current instanceof TimelineFragment &&
                clazz == UserDescriptionFragment.class) {
            if (mEditDesFragment == null) {
                mEditDesFragment = new UserDescriptionFragment();
                mEditDesFragment.setListener(
                        new XBaseFragmentListener() {
                            @Override
                            public void onEnter() {
                                refreshToolBar();
                                mAppBar.setExpanded(false, true);
                                ViewUtil.animateExpand(mAccountLine, false);
                                ViewUtil.animateFadeInOut(mAccountDesView, true);
                            }

                            @Override
                            public void onLoading(boolean show) {
                                showProgress(show);
                            }

                            @Override
                            public void onFinish(boolean result, Object data) {
                                showProgress(false);
                                if (result && data != null) {
                                    // 刷新用户的description
                                    final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                                            .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
                                    mAccountDesView.setText(XStringUtil.list2String(user.descriptions, ", "));
                                    // 模拟返回按钮
                                    onBackPressed();
                                }
                            }
                        });
            }
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_layout, mEditDesFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        } else if (current instanceof TimelineFragment &&
                clazz == MemoryAddFragment.class) {
            // 每次都新建一个MemoryAddFragment
            mAddFragment = new MemoryAddFragment();
            mAddFragment.setListener(
                    new XBaseFragmentListener() {
                        @Override
                        public void onEnter() {
                            refreshToolBar();
                            if (!mCollapsed) {
                                mAppBar.setExpanded(false, true);
                            }
                            ViewUtil.animateExpand(mAccountLine, false);
                        }

                        @Override
                        public void onLoading(boolean show) {
                            showProgress(show);
                        }

                        @Override
                        public void onRefresh(int refreshEvent, Object data) {
                            switch (refreshEvent) {
                                case MemoryAddFragment.REFRESH_SET_COVER:
                                    Intent target = FileUtils.createGetContentIntent();
                                    Intent intent = Intent.createChooser(target, getString(R.string.info_choose_memory_cover));
                                    try {
                                        startActivityForResult(intent, TimelineActivity.COVER_REQUEST_CODE);
                                    } catch (ActivityNotFoundException ex) {
                                        Toast.makeText(TimelineActivity.this, getString(R.string.error_select_picture),
                                                Toast.LENGTH_LONG).show();
                                    }
                                    break;
                            }
                        }

                        @Override
                        public void onFinish ( boolean result, Object data){
                            showProgress(false);
                            if (result && data != null) {
                                Memory memory = (Memory) data;
                                XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                                        XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
                                source.add(memory);
                                source.sort(Memory.comparator);
                                // 模拟返回按钮
                                onBackPressed();
                            }
                        }
                    });
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_layout, mAddFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }
}
