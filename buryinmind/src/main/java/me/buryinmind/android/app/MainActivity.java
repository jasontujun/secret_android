package me.buryinmind.android.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.utils.XStringUtil;

import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;

public class MainActivity extends AppCompatActivity {

    private static class TimelineUnit {
        public static final int TYPE_TAG = 1;
        public static final int TYPE_NODE = 2;
        int type;
        int age;
        Memory memory;

        public TimelineUnit(){
            this.type = TYPE_TAG;
        }

        public TimelineUnit(int age){
            this.type = TYPE_TAG;
            this.age = age;
        }
    }

    private static final int REFRESH_COMPLETE = 1;
    private static final String TAG = "BIM_MainActivity";

    private Toolbar mToolBar;
    private AppBarLayout mAppBar;
    private CollapsingToolbarLayout mCollapsedLayout;
    private SwipeRefreshLayout mRefreshLayout;
    private TimelineAdapter mAdapter;
    private boolean mCollapsed;
    private List<TimelineUnit> timeList = new ArrayList<>();


    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case REFRESH_COMPLETE:
                    timeList.add(new TimelineUnit((int) (Math.random() *100)));
                    mAdapter.notifyDataSetChanged();
                    mRefreshLayout.setRefreshing(false);
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
        mCollapsedLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.timeline_refresh_layout);
        RecyclerView mAccountListView = (RecyclerView) findViewById(R.id.timeline_list);
        ImageView mProfileBackground = (ImageView) findViewById(R.id.account_profile_bg);
        ImageView mAccountHeadView = (ImageView) findViewById(R.id.account_head_img);
        TextView mAccountNameView = (TextView) findViewById(R.id.account_name_txt);
        TextView mAccountDesView = (TextView) findViewById(R.id.account_des_txt);

        setSupportActionBar(mToolBar);
        // init refresh layout
        mRefreshLayout.setColorSchemeResources(R.color.green, R.color.red, R.color.yellow);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Your refresh code here
                // 本地测试数据
                mHandler.sendEmptyMessageDelayed(REFRESH_COMPLETE, 2000);
            }
        });
        // init clickable expand/collapse
        mToolBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XLog.d(TAG, "toolbar.onClick");
                if (mCollapsed) {
                    mAppBar.setExpanded(true, true);
                } else {
                    mAppBar.setExpanded(false, true);
                }
            }
        });
        mAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset <= mToolBar.getHeight() - mCollapsedLayout.getHeight()) {
                    // 已经彻底折叠
                    XLog.d(TAG, "mAppBar already collapsed");
                    mCollapsed = true;
                } else {
                    XLog.d(TAG, "mAppBar expand");
                    mCollapsed = false;
                }
            }
        });
        // init user profile
        GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
        User user = source.getUser();
        if (user != null) {
            Glide.with(MainActivity.this)
                    .load(R.drawable.test)
                    .into(mProfileBackground);
            Glide.with(MainActivity.this)
                    .load(R.drawable.test)
                    .transform(new CircleTransform(MainActivity.this))
                    .placeholder(R.drawable.headicon_active)
                    .into(mAccountHeadView);
            mAccountNameView.setText(user.name);
            mAccountDesView.setText(XStringUtil.list2String(user.descriptions, " ,"));
        }

        // TODO TEST
        timeList.add(new TimelineUnit(3));
        timeList.add(new TimelineUnit(7));
        timeList.add(new TimelineUnit(13));
        timeList.add(new TimelineUnit(18));
        timeList.add(new TimelineUnit(24));
        timeList.add(new TimelineUnit(26));
        timeList.add(new TimelineUnit(45));
        timeList.add(new TimelineUnit(55));
        timeList.add(new TimelineUnit(65));
        timeList.add(new TimelineUnit(76));
        timeList.add(new TimelineUnit(88));
        timeList.add(new TimelineUnit(99));
        mAdapter = new TimelineAdapter(timeList);
        mAccountListView.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }


    public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
        private final List<TimelineUnit> mValues;

        public TimelineAdapter(List<TimelineUnit> items) {
            mValues = items;
        }

        @Override
        public int getItemViewType(int position) {
            return mValues.get(position).type;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int resId = viewType == TimelineUnit.TYPE_TAG ?
                    R.layout.item_timeline_tag : R.layout.item_timeline_node;
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(resId, parent, false);
            return new ViewHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            int type = getItemViewType(position);
            holder.mItem = mValues.get(position);
            if (type == TimelineUnit.TYPE_TAG) {
                holder.mAgeView.setText(String.valueOf(holder.mItem.age));
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "点击Tag:" + holder.mItem.age, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                holder.mMemoryNameView.setText(holder.mItem.memory.name);
                holder.mMemoryDateView.setText(XStringUtil.date2str(holder.mItem.memory.happenTime));
                if (holder.mItem.memory.secrets != null && holder.mItem.memory.secrets.size() > 0) {
                    holder.mMemoryCoverLayout.setVisibility(View.VISIBLE);
//                    holder.mMemoryCover// TODO 加载图片
                } else {
                    holder.mMemoryCoverLayout.setVisibility(View.GONE);
                }
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "点击Memory:" + holder.mItem.memory.name, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mAgeView;
            public final TextView mAgeDesView;
            public final TextView mMemoryNameView;
            public final TextView mMemoryDateView;
            public final View mMemoryCoverLayout;
            public final ImageView mMemoryCover;
            public TimelineUnit mItem;

            public ViewHolder(View view, int viewType) {
                super(view);
                mView = view;
                if (viewType == TimelineUnit.TYPE_TAG) {
                    mAgeView = (TextView) view.findViewById(R.id.timeline_tag);
                    mAgeDesView = (TextView) view.findViewById(R.id.timeline_tag_txt);
                    mMemoryNameView = null;
                    mMemoryDateView = null;
                    mMemoryCoverLayout = null;
                    mMemoryCover = null;
                } else {
                    mAgeView = null;
                    mAgeDesView = null;
                    mMemoryNameView = (TextView) view.findViewById(R.id.timeline_node_txt);
                    mMemoryDateView = (TextView) view.findViewById(R.id.timeline_node_date);
                    mMemoryCoverLayout = view.findViewById(R.id.timeline_node_img_layout);
                    mMemoryCover = (ImageView) view.findViewById(R.id.timeline_node_img);
                }
            }
        }
    }

    public static class CircleTransform extends BitmapTransformation {
        public CircleTransform(Context context) {
            super(context);
        }

        @Override protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
            return circleCrop(pool, toTransform);
        }

        private static Bitmap circleCrop(BitmapPool pool, Bitmap source) {
            if (source == null) return null;

            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            // TODO this could be acquired from the pool too
            Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);

            Bitmap result = pool.get(size, size, Bitmap.Config.ARGB_8888);
            if (result == null) {
                result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(result);
            Paint paint = new Paint();
            paint.setShader(new BitmapShader(squared, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
            paint.setAntiAlias(true);
            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);
            return result;
        }

        @Override public String getId() {
            return getClass().getName();
        }
    }
}
