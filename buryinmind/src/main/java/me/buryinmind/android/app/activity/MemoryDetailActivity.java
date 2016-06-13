package me.buryinmind.android.app.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.android.data.XListIdDBDataSourceImpl;
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.controller.ProgressListener;
import me.buryinmind.android.app.data.SecretSource;
import me.buryinmind.android.app.dialog.ConfirmDialog;
import me.buryinmind.android.app.dialog.DialogListener;
import me.buryinmind.android.app.fragment.FragmentInteractListener;
import me.buryinmind.android.app.fragment.MemoryDetailFragment;
import me.buryinmind.android.app.fragment.PostMemoryFragment;
import me.buryinmind.android.app.fragment.SearchFriendsFragment;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.uicontrol.XListAdapter;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.TimeUtil;
import me.buryinmind.android.app.uicontrol.XViewHolder;
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
//                XLog.d(TAG, "onOffsetChanged().verticalOffset=" + verticalOffset
//                        + ",mToolBar.getHeight()=" + mToolBar.getHeight()
//                        + ",mCollapsedLayout.getHeight()=" + mCollapsedLayout.getHeight());
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
        if (!mMemory.ownerId.equals(mMemory.authorId)) {
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

        if (mSecretsFragment == null) {
            mSecretsFragment = (MemoryDetailFragment) createSecretsFragment();
        }
        getFragmentManager().beginTransaction()
                .add(R.id.content_layout, mSecretsFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_memory, menu);
        mAddBtn = menu.getItem(0);
        mPostBtn = menu.getItem(1);
        mPostBtn.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                XLog.d(TAG, "click back btn!");
                onKeyDown(KeyEvent.KEYCODE_BACK, null);
                return true;
            case R.id.action_add:
                XLog.d(TAG, "click add btn!");
                Intent intent = new Intent(MemoryDetailActivity.this, ImageSelectorActivity.class);
                startActivityForResult(intent, IMAGE_REQUEST_CODE);
                return true;
            case  R.id.action_post:
                XLog.d(TAG, "click post btn!");
                goToNext(SearchFriendsFragment.class, null);
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
            ViewUtil.hidInputMethod(this);
        }
        return super .onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    backToLast();
                } else {
                    onBackPressed();
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
                break;
        }
        return false;
    }

    private void goToNext(Class<?> clazz, Object data) {
        Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
        if (current == null)
            return;
        if (current instanceof MemoryDetailFragment &&
                clazz == SearchFriendsFragment.class) {
            mPostBtn.setVisible(false);
            collapseToolBar();
            if (mFriendsFragment == null) {
                mFriendsFragment = (SearchFriendsFragment) createFriendsFragment();
            }
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_layout, mFriendsFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        } else if (current instanceof SearchFriendsFragment &&
                clazz == PostMemoryFragment.class) {
            expandToolBar();
            User user = (User) data;
            if (mPostFragment == null) {
                mPostFragment = (PostMemoryFragment) createPostFragment(user);
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

    private void backToLast() {
        Fragment current = getFragmentManager().findFragmentById(R.id.content_layout);
        if (current == null)
            return;
        getFragmentManager().popBackStack();
        if (current instanceof SearchFriendsFragment) {
            refreshShareBtn();
            expandToolBar();
            mSecretsFragment.needScrollToTop();
        } else if (current instanceof PostMemoryFragment) {
            collapseToolBar();
        }
    }

    private Fragment createSecretsFragment() {
        Fragment fragment = new MemoryDetailFragment();
        Bundle arguments = new Bundle();
        arguments.putString(MemoryDetailFragment.KEY_MID, mMemory.mid);
        fragment.setArguments(arguments);
        return fragment;
    }

    private Fragment createFriendsFragment() {
        SearchFriendsFragment fragment = new SearchFriendsFragment();
        Bundle arguments = new Bundle();
        fragment.setListener(
                new FragmentInteractListener() {
                    @Override
                    public void onLoading() {
                    }

                    @Override
                    public void onBack() {
                    }

                    @Override
                    public void onFinish(boolean result, Object data) {
                        if (result) {
                            goToNext(PostMemoryFragment.class, data);
                        }
                    }
                });
        fragment.setArguments(arguments);
        return fragment;
    }

    private Fragment createPostFragment(User user) {
        PostMemoryFragment fragment = new PostMemoryFragment();
        fragment.setListener(
                new FragmentInteractListener() {
                    @Override
                    public void onLoading() {
                    }

                    @Override
                    public void onBack() {
                    }

                    @Override
                    public void onFinish(boolean result, Object data) {
                        if (result) {
                            // TODO 发送成功,重新进入Memory详情界面
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

    public void collapseToolBar() {
        if (!mCollapsed) {
            XLog.d(TAG, "try collapseToolBar..");
            mAppBar.setExpanded(false, true);
        }
    }

    public void expandToolBar() {
        mAppBar.setExpanded(true, true);
    }

    public void refreshShareBtn() {
        if (mMemory.secrets.size() > 0) {
            mPostBtn.setVisible(true);
        } else {
            mPostBtn.setVisible(false);
        }
    }
}
