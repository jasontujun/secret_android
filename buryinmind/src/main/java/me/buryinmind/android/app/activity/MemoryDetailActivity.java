package me.buryinmind.android.app.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.utils.XStringUtil;

import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.fragment.XBaseFragmentListener;
import me.buryinmind.android.app.fragment.XFragmentListener;
import me.buryinmind.android.app.fragment.MemoryDetailFragment;
import me.buryinmind.android.app.fragment.PostMemoryFragment;
import me.buryinmind.android.app.fragment.SearchFriendsFragment;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.TimeUtil;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/5/15.
 */
public class MemoryDetailActivity extends AppCompatActivity {

    private static final String TAG = MemoryDetailActivity.class.getSimpleName();
    private static final int IMAGE_REQUEST_CODE = 9900;

    private MemoryDetailFragment mSecretsFragment;
    private SearchFriendsFragment mFriendsFragment;
    private PostMemoryFragment mPostFragment;

    private CollapsingToolbarLayout mCollapsedLayout;
    private AppBarLayout mAppBar;
    private Toolbar mToolBar;
    private MenuItem mAddBtn;
    private MenuItem mPostBtn;
    private MenuItem mDoneBtn;

    private boolean mCollapsed = false;
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

        mCollapsedLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
        ImageView mMemoryProfileView = (ImageView) findViewById(R.id.memory_profile_bg);
        TextView mMemoryTime = (TextView) findViewById(R.id.memory_time_view);
        View mAuthorLayout = findViewById(R.id.memory_author_layout);
        ImageView mAuthorHeader = (ImageView) findViewById(R.id.account_head_img);
        TextView mAuthorName = (TextView) findViewById(R.id.account_name_txt);

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

        // init profile and author
        if (!XStringUtil.isEmpty(mMemory.coverUrl)) {
            Glide.with(MemoryDetailActivity.this)
                    .load(mMemory.coverUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .priority(Priority.HIGH)
                    .placeholder(R.color.darkGray)
                    .error(R.color.darkGray)
                    .into(mMemoryProfileView);
        } else {
            mMemoryProfileView.setImageResource(R.color.darkGray);
        }
        mMemoryProfileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO 修改Memory封面
            }
        });
        mMemoryTime.setText(String.format(getResources().getString(R.string.info_memory_time),
                XStringUtil.calendar2str(TimeUtil.getCalendar(mMemory.happenTime), ".")));
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
                startActivityForResult(intent, IMAGE_REQUEST_CODE);
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
            case IMAGE_REQUEST_CODE:
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
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            supportFinishAfterTransition();
        }
    }

    private void refreshMenu() {
        Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
        if (current == null) {
            return;
        }
        if (current instanceof MemoryDetailFragment) {
            mAddBtn.setVisible(true);
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

    private void goToNext(Class<?> clazz, Object data) {
        Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
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
                    case MemoryDetailFragment.REFRESH_MENU:
                        refreshMenu();
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
            public void onFinish(boolean result, Object data) {
                if (result) {
                    // 发送成功,重新进入Memory详情界面
                    Intent intent = new Intent(MemoryDetailActivity.this, MemoryDetailActivity.class);
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
