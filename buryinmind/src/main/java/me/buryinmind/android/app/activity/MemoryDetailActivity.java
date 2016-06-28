package me.buryinmind.android.app.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.controller.ProgressListener;
import me.buryinmind.android.app.fragment.XBaseFragmentListener;
import me.buryinmind.android.app.fragment.MemoryDetailFragment;
import me.buryinmind.android.app.fragment.PostMemoryFragment;
import me.buryinmind.android.app.fragment.SearchFriendsFragment;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.uicontrol.XLinearLayoutManager;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.FileUtils;
import me.buryinmind.android.app.util.ImageUtil;
import me.buryinmind.android.app.util.TimeUtil;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/5/15.
 */
public class MemoryDetailActivity extends XActivity {

    private static final String TAG = MemoryDetailActivity.class.getSimpleName();
    private static final int SECRET_REQUEST_CODE = 9900;
    private static final int COVER_REQUEST_CODE = 9901;

    private MemoryDetailFragment mSecretsFragment;
    private SearchFriendsFragment mFriendsFragment;
    private PostMemoryFragment mPostFragment;

    private View mProgressView;
    private View mContentLayout;
    private CollapsingToolbarLayout mCollapsedLayout;
    private AppBarLayout mAppBar;
    private ImageView mMemoryCoverView;
    private View mMemoryCoverMask;
    private View mMemoryCoverLayout;
    private TextView mMemoryTimeView;
    private View mAuthorLayout;
    private ImageView mAuthorHeader;
    private TextView mAuthorName;
    private Toolbar mToolBar;
    private MenuItem mAddBtn;
    private MenuItem mPostBtn;
    private MenuItem mDoneBtn;

    private boolean mCollapsed = false;
    private boolean mWaiting = false;
    private Memory mMemory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_detail);

        // init memory data
        Intent intent = getIntent();
        String memoryId = intent.getStringExtra("mid");
        if (XStringUtil.isEmpty(memoryId)) {
            return;// error
        }
        XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
        mMemory = source.getById(memoryId);
        if (mMemory == null) {
            return;// error
        }

        mProgressView = findViewById(R.id.loading_progress);
        mContentLayout = findViewById(R.id.content_layout);
        mCollapsedLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
        mMemoryCoverView = (ImageView) findViewById(R.id.memory_cover_image);
        mMemoryCoverMask = findViewById(R.id.memory_cover_image_mask);
        mMemoryCoverLayout = findViewById(R.id.memory_cover_layout);
        mMemoryTimeView = (TextView) findViewById(R.id.memory_time_view);
        mAuthorLayout = findViewById(R.id.memory_author_layout);
        mAuthorHeader = (ImageView) findViewById(R.id.memory_author_head_img);
        mAuthorName = (TextView) findViewById(R.id.memory_author_name_txt);

        // init toolbar
        mCollapsedLayout.setTitle(mMemory.name);
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

        // init cover
        refreshCover(mMemory.coverUrl);
        if (mMemory.editable) {
            mMemoryCoverLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
                    if (!(current instanceof MemoryDetailFragment)) {
                        return;
                    }
                    if (mWaiting) {
                        Toast.makeText(MemoryDetailActivity.this, R.string.error_loading, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 修改Memory封面
                    Intent target = FileUtils.createGetContentIntent();
                    Intent intent = Intent.createChooser(target, getString(R.string.info_choose_memory_cover));
                    try {
                        startActivityForResult(intent, COVER_REQUEST_CODE);
                    } catch (ActivityNotFoundException ex) {
                        Toast.makeText(MemoryDetailActivity.this, getString(R.string.error_select_picture),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        // init author and info
        mMemoryTimeView.setText(String.format(getResources().getString(R.string.info_memory_time),
                XStringUtil.calendar2str(TimeUtil.getCalendar(mMemory.happenStartTime), ".")));
        if (mMemory.editable) {
            mMemoryTimeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
                    if (!(current instanceof MemoryDetailFragment)) {
                        return;
                    }
                    // TODO 修改Memory发生的时间
                    Toast.makeText(MemoryDetailActivity.this, "修改Memory发生的时间", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (!mMemory.authorId.equals(mMemory.ownerId)) {
            mAuthorLayout.setVisibility(View.VISIBLE);
            Glide.with(MemoryDetailActivity.this)
                    .load(ApiUtil.getIdUrl(mMemory.authorId))
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.headicon_default)
                    .into(mAuthorHeader);
            mAuthorName.setText(mMemory.authorName);
        } else {
            mAuthorLayout.setVisibility(View.GONE);
        }

        // 为了先执行onCreateOptionsMenu(),所以延迟添加Fragment
        mCollapsedLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSecretsFragment == null) {
                    mSecretsFragment = (MemoryDetailFragment) createSecretsFragment();
                }
                getFragmentManager().beginTransaction()
                        .add(R.id.content_layout, mSecretsFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }
        }, 50);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_memory, menu);
        mAddBtn = menu.getItem(0);
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
            case R.id.action_add:
                Intent intent = new Intent(MemoryDetailActivity.this, ImageSelectorActivity.class);
                startActivityForResult(intent, SECRET_REQUEST_CODE);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SECRET_REQUEST_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    List<String> selectedImages = (List<String>) data.getExtras().getSerializable("result");
                    if (selectedImages == null || selectedImages.size() == 0)
                        break;
                    final List<Secret> newSecrets = new ArrayList<Secret>();
                    for (String imagePath : selectedImages) {
                        Secret se = Secret.createLocal(mMemory.mid, imagePath);
                        se.order = mMemory.secrets.size() + newSecrets.size();
                        newSecrets.add(se);
                    }
                    mSecretsFragment.addSecret(newSecrets);
                }
                break;
            case COVER_REQUEST_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    final String path = FileUtils.getPath(this, data.getData());
                    if (XStringUtil.isEmpty(path)) {
                        Toast.makeText(this, getString(R.string.error_select_picture),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (mWaiting) {
                        return;
                    }
                    mWaiting = true;
                    // 如果设置了封面图片，则压缩并上传图片
                    ImageUtil.compressAndUploadImage(MemoryDetailActivity.this,
                            ApiUtil.getMemoryCoverToken(mMemory.mid), path,
                            new ProgressListener<String>() {
                                @Override
                                public void onProgress(String filePath,
                                                       long completeSize, long totalSize) {
                                    // TODO 显示上传进度？
                                }

                                @Override
                                public void onResult(boolean result, final String key) {
                                    if (result) {
                                        // 上传成功，回调服务器
                                        final String url = ApiUtil.PUBLIC_DOMAIN + "/" + key;
                                        final int[] dimension = ImageUtil.getDimension(path);
                                        putAsyncTask(MyApplication.getAsyncHttp().execute(
                                                ApiUtil.updateMemoryCover(mMemory.mid,
                                                        url, dimension[0], dimension[1]),
                                                new XAsyncHttp.Listener() {
                                                    @Override
                                                    public void onCancelled() {
                                                    }

                                                    @Override
                                                    public void onNetworkError() {
                                                        Toast.makeText(MemoryDetailActivity.this, R.string.error_upload_picture, Toast.LENGTH_SHORT).show();
                                                        mWaiting = false;
                                                    }

                                                    @Override
                                                    public void onFinishError(XHttpResponse xHttpResponse) {
                                                        Toast.makeText(MemoryDetailActivity.this, R.string.error_upload_picture, Toast.LENGTH_SHORT).show();
                                                        mWaiting = false;
                                                    }

                                                    @Override
                                                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                                                        mMemory.coverUrl = url;
                                                        mMemory.coverWidth = dimension[0];
                                                        mMemory.coverHeight = dimension[1];
                                                        mMemoryCoverMask.setVisibility(View.VISIBLE);
                                                        mWaiting = false;
                                                        // 刷新封面图片
                                                        refreshCover(path);
                                                    }
                                                }));
                                    } else {
                                        // 上传失败
                                        Toast.makeText(MemoryDetailActivity.this, R.string.error_upload_picture, Toast.LENGTH_SHORT).show();
                                        mWaiting = false;
                                    }
                                }
                            });
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
    protected Fragment getCurrentFragment() {
        return getFragmentManager().findFragmentById(R.id.content_layout);
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
        if (current instanceof MemoryDetailFragment) {
            if (mMemory.editable) {
                mAddBtn.setVisible(true);
            } else {
                mAddBtn.setVisible(false);
            }
            if (mMemory.secrets.size() > 0) {
                mPostBtn.setVisible(true);
            } else {
                mPostBtn.setVisible(false);
            }
            mDoneBtn.setVisible(false);
        } else if (current instanceof SearchFriendsFragment) {
            mAddBtn.setVisible(false);
            mPostBtn.setVisible(false);
            mDoneBtn.setVisible(false);
        } else if (current instanceof PostMemoryFragment) {
            mAddBtn.setVisible(false);
            mPostBtn.setVisible(false);
            mDoneBtn.setVisible(true);
        }
    }

    private void refreshCover(String url) {
        if (XStringUtil.isEmpty(url)) {
            mMemoryCoverMask.setVisibility(View.GONE);
            mMemoryCoverView.setImageResource(R.color.darkGray);
        } else {
            mMemoryCoverMask.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .priority(Priority.HIGH)
                    .placeholder(R.color.darkGray)
                    .error(R.color.darkGray)
                    .into(mMemoryCoverView);
        }
    }

    private void goToNext(Class<?> clazz, Object data) {
        Fragment current = getCurrentFragment();
        if (current == null)
            return;
        if (current instanceof MemoryDetailFragment &&
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
        final MemoryDetailFragment fragment = new MemoryDetailFragment();
        fragment.setListener(new XBaseFragmentListener() {
            @Override
            public void onEnter() {
                refreshMenu();
                mAppBar.setExpanded(true, true);
                fragment.needScrollToTop();
            }

            @Override
            public void onLoading(boolean show) {
                showProgress(show);
            }

            @Override
            public void onRefresh(int refreshEvent, Object data) {
                switch (refreshEvent) {
                    case MemoryDetailFragment.REFRESH_COLLAPSE:
                        if (!mCollapsed) {
                            XLog.d(TAG, "try collapseToolBar..");
                            mAppBar.setExpanded(false, true);
                        }
                        break;
                    case MemoryDetailFragment.REFRESH_EXPAND:
                        mAppBar.setExpanded(true, true);
                        break;
                    case MemoryDetailFragment.REFRESH_DATA:
                        refreshMenu();
                        break;
                    case MemoryDetailFragment.REFRESH_OUT_GIFT:
                        break;
                }
            }
        });
        Bundle arguments = new Bundle();
        arguments.putString(MemoryDetailFragment.KEY_MID, mMemory.mid);
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
                        if (!mCollapsed) {
                            XLog.d(TAG, "try collapseToolBar..");
                            mAppBar.setExpanded(false, true);
                        }
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
                mAppBar.setExpanded(true, true);
            }

            @Override
            public void onLoading(boolean show) {
                showProgress(show);
            }

            @Override
            public void onFinish(boolean result, Object data) {
                if (result) {
                    // 发送成功,重新进入Memory详情界面
                    Intent intent = new Intent(MemoryDetailActivity.this, MemoryGiftActivity.class);
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
}
