package me.buryinmind.android.app;

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
import android.widget.TextView;
import android.widget.Toast;

import com.tj.xengine.android.utils.XLog;

import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.model.Memory;

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
                holder.mAgeView.setText("" + holder.mItem.age);
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "点击Tag:" + holder.mItem.age, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                holder.mMemoryNameView.setText(holder.mItem.memory.name);
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
            public final TextView mMemoryNameView;
            public TimelineUnit mItem;

            public ViewHolder(View view, int viewType) {
                super(view);
                mView = view;
                if (viewType == TimelineUnit.TYPE_TAG) {
                    mAgeView = (TextView) view.findViewById(R.id.timeline_tag_txt);
                    mMemoryNameView = null;
                } else {
                    mAgeView = null;
                    mMemoryNameView = (TextView) view.findViewById(R.id.timeline_node_txt);
                }
            }
        }
    }
}
