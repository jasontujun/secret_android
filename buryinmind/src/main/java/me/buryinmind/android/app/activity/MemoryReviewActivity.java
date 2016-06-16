package me.buryinmind.android.app.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lsjwzh.widget.recyclerviewpager.RecyclerViewPager;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.utils.XStringUtil;

import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.adapter.XListAdapter;
import me.buryinmind.android.app.adapter.XViewHolder;

/**
 * Created by jasontujun on 2016/5/22.
 */
public class MemoryReviewActivity extends AppCompatActivity {

    private static final String TAG = MemoryReviewActivity.class.getSimpleName();

    private Toolbar mToolBar;
    private TextView mToolBarTitle;
    private View mToolBarLayout;

    private boolean mShowToolBar;
    private Memory mMemory;
    private RecyclerViewPager mRecyclerPager;
    private ViewAdapter mViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_detail);

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

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBarLayout = findViewById(R.id.toolbar_layout);
        mToolBarTitle = (TextView) findViewById(R.id.toolbar_title);
        mRecyclerPager = (RecyclerViewPager) findViewById(R.id.secret_view_list);

        // init toolbar
        mShowToolBar = true;
        mToolBar.setTitle("");
        mToolBarTitle.setText(mMemory.name);
        setSupportActionBar(mToolBar);


        // init view pager
        mRecyclerPager.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerPager.setLongClickable(true);
        mRecyclerPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int scrollState) {
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int i, int i2) {
                int childCount = mRecyclerPager.getChildCount();
                int width = mRecyclerPager.getChildAt(0).getWidth();
                int padding = (mRecyclerPager.getWidth() - width) / 2;

                for (int j = 0; j < childCount; j++) {
                    View v = recyclerView.getChildAt(j);
                    //往左 从 padding 到 -(v.getWidth()-padding) 的过程中，由大到小
                    float rate = 0;
                    ;
                    if (v.getLeft() <= padding) {
                        if (v.getLeft() >= padding - v.getWidth()) {
                            rate = (padding - v.getLeft()) * 1f / v.getWidth();
                        } else {
                            rate = 1;
                        }
                        v.setScaleY(1 - rate * 0.1f);
                        v.setScaleX(1 - rate * 0.1f);

                    } else {
                        //往右 从 padding 到 recyclerView.getWidth()-padding 的过程中，由大到小
                        if (v.getLeft() <= recyclerView.getWidth() - padding) {
                            rate = (recyclerView.getWidth() - padding - v.getLeft()) * 1f / v.getWidth();
                        }
                        v.setScaleY(0.9f + rate * 0.1f);
                        v.setScaleX(0.9f + rate * 0.1f);
                    }
                }
            }
        });
        mViewAdapter = new ViewAdapter(mMemory.secrets);
        mRecyclerPager.setAdapter(mViewAdapter);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        final int position = mRecyclerPager.getCurrentPosition();
        super.onConfigurationChanged(newConfig);
        mRecyclerPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                XLog.d(TAG, "scrollToPosition p=" + position);
                mRecyclerPager.scrollToPosition(position);
            }
        }, 100);
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



    private class ViewAdapter extends XListAdapter<Secret> {

        public ViewAdapter(List<Secret> items) {
            super(R.layout.item_view_secret, items);
        }

        @Override
        public void onBindViewHolder(final XViewHolder holder, final int position) {
            final Secret item = getData().get(position);
            Glide.with(MemoryReviewActivity.this)
                    .load(item.localPath)
                    .error(R.drawable.profile_default)
                    .into(holder.getView(R.id.secret_item_img, ImageView.class));
        }
    }
}
