package me.buryinmind.android.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.tj.xengine.android.data.listener.XHandlerIdDataSourceListener;
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.CircleTransform;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BIM_MainActivity";
    private static final String DATEPICKER_TAG = "datepicker";

    private View mProgressView;
    private View mContentView;
    private Toolbar mToolBar;
    private AppBarLayout mAppBar;
    private CollapsingToolbarLayout mCollapsedLayout;
    private RecyclerView mTimelineView;
    private TimelineAdapter mAdapter;
    private View mAddBtn;
    private View mBirthdayView;
    private Button mBirthdayBtn;

    private boolean mCollapsed;

    private boolean mWaiting;
    private boolean mAddingMemory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressView = findViewById(R.id.loading_progress);
        mContentView = findViewById(R.id.content_layout);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
        mCollapsedLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mTimelineView = (RecyclerView) findViewById(R.id.timeline_list);
        mAddBtn = findViewById(R.id.timeline_add_btn);
        mBirthdayView = findViewById(R.id.timeline_birthday_layout);
        mBirthdayBtn = (Button) findViewById(R.id.timeline_birthday_btn);
        ImageView mProfileBackground = (ImageView) findViewById(R.id.account_profile_bg);
        ImageView mAccountHeadView = (ImageView) findViewById(R.id.account_head_img);
        TextView mAccountNameView = (TextView) findViewById(R.id.account_name_txt);
        TextView mAccountDesView = (TextView) findViewById(R.id.account_des_txt);

        setSupportActionBar(mToolBar);
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
        mTimelineView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!ViewCompat.canScrollVertically(recyclerView, -1)) {
                    // 已经滑到顶部，不能再向上滑了
                    XLog.d(TAG, "mTimelineView already scroll to top! expand appbar!");
                    mAppBar.setExpanded(true, true);
                }
            }
        });

        // init user profile
        GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
        final User user = source.getUser();
        if (user == null) {
            return;// TODO erro message
        }
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

        // init add button
        mAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAddingMemory) {
                    mAddingMemory = false;
                    Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate_back);
                    animation.setFillAfter(true);
                    mAddBtn.startAnimation(animation);
                } else {
                    mAddingMemory = true;
                    Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate_start);
                    animation.setFillAfter(true);
                    mAddBtn.startAnimation(animation);
                }
            }
        });

        // register data listener
        final XListIdDataSourceImpl<Memory> memorySource = (XListIdDataSourceImpl<Memory>)
                XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
        memorySource.registerListener(new XHandlerIdDataSourceListener<Memory>() {
            @Override
            public void onReplaceInUI(List<Memory> list, List<Memory> list1) {

            }

            @Override
            public void onChangeInUI() {

            }

            @Override
            public void onAddInUI(Memory memory) {
                XListIdDataSourceImpl<Memory> memorySource = (XListIdDataSourceImpl<Memory>)
                        XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
                int pos = memorySource.getIndexById(memory.mid);

//                mAdapter.notifyItemInserted();
            }

            @Override
            public void onAddAllInUI(List<Memory> list) {

            }

            @Override
            public void onDeleteInUI(Memory memory) {

            }

            @Override
            public void onDeleteInUI(List<Memory> list) {

            }
        });
        // init first ui
        if (user.bornTime == 0) {
            // TODO 让用户设置出生日期,再生成时间线
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            final DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                            Calendar birthCal = Calendar.getInstance();
                            birthCal.set(year, month, day);
                            birthCal.setTimeZone(TimeZone.getTimeZone("GMT"));// 统一改成GMT时区,再上传服务器
                            final long bornTime = birthCal.getTimeInMillis();
                            XLog.d(TAG, "select year=" + year + ",month=" + month + ",day=" + day + ",bornTime=" + bornTime);
                            updateBornTime(user.uid, bornTime, new ApiUtil.SimpleListener() {
                                @Override
                                public void onResult(boolean result) {
                                    if (result) {
                                        user.bornTime = bornTime;
                                        // 生成时间线
                                        mBirthdayView.setVisibility(View.GONE);
                                        mTimelineView.setVisibility(View.VISIBLE);
                                        mAddBtn.setVisibility(View.VISIBLE);
                                        // init list adapter
                                        Calendar bornCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                                        bornCal.setTimeInMillis(user.bornTime);
                                        bornCal.setTimeZone(TimeZone.getDefault());
                                        mAdapter = new TimelineAdapter(bornCal, memorySource.copyAll());
                                        mTimelineView.setAdapter(mAdapter);
                                        mTimelineView.setItemAnimator(new DefaultItemAnimator());
                                        // request memory data
                                        requestMemoryList(user.uid);
                                    }
                                }
                            });
                        }
                    },
                    year, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), false);
            datePickerDialog.setYearRange(1902, year);
            mBirthdayView.setVisibility(View.VISIBLE);
            mBirthdayBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    datePickerDialog.show(getSupportFragmentManager(), DATEPICKER_TAG);
                }
            });
            mTimelineView.setVisibility(View.GONE);
            mAddBtn.setVisibility(View.GONE);
        } else {
            // 生成时间线
            mBirthdayView.setVisibility(View.GONE);
            mTimelineView.setVisibility(View.VISIBLE);
            mAddBtn.setVisibility(View.VISIBLE);
            // init list adapter
            Calendar bornCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            bornCal.setTimeInMillis(user.bornTime);
            bornCal.setTimeZone(TimeZone.getDefault());
            mAdapter = new TimelineAdapter(bornCal, memorySource.copyAll());
            mTimelineView.setAdapter(mAdapter);
            mTimelineView.setItemAnimator(new DefaultItemAnimator());
            // request memory data
            requestMemoryList(user.uid);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }


    private void showProgress(final boolean show) {
        animateFadeInOut(mContentView, show);
        animateFadeInOut(mProgressView, !show);
    }

    private static void animateFadeInOut(final View view, final boolean fadeout) {
        view.setVisibility(fadeout ? View.GONE : View.VISIBLE);
        view.animate().setDuration(200).alpha(
                fadeout ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(fadeout ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void requestMemoryList(String uid) {
        if (mWaiting)
            return;
        mWaiting = true;
        showProgress(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.getMemoryList(uid),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(MainActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(MainActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONArray jsonArray) {
                        mWaiting = false;
                        showProgress(false);
                        List<Memory> memories = Memory.fromJson(jsonArray);
                        XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                                XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
                        source.addAll(memories);
                        source.sort(new Memory.Comp());
                    }
                });
    }

    private void updateBornTime(String uid, long bornTime, final ApiUtil.SimpleListener listener) {
        if (mWaiting)
            return;
        mWaiting = true;
        showProgress(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.updateBornTime(uid, bornTime),
                new XAsyncHttp.Listener() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(MainActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onResult(false);
                        }
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(MainActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onResult(false);
                        }
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                        mWaiting = false;
                        showProgress(false);
                        if (listener != null) {
                            listener.onResult(true);
                        }
                    }
                });
    }



    private class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

        private static final int TYPE_HEADER = 1;
        private static final int TYPE_FOOTER = 2;
        private static final int TYPE_NODE = 3;

        private Calendar mBornCal;
        private List<Memory> mValues;

        public TimelineAdapter(Calendar bornCal, List<Memory> items) {
            mBornCal = bornCal;
            mValues = items;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return TYPE_HEADER;
            else if (position == mValues.size() + 1)
                return TYPE_FOOTER;
            else
                return TYPE_NODE;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int resId;
            if (viewType == TYPE_HEADER)
                resId = R.layout.item_timeline_header;
            else if (viewType == TYPE_FOOTER)
                resId = R.layout.item_timeline_footer;
            else
                resId = R.layout.item_timeline_node;
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(resId, parent, false);
            return new ViewHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            int type = getItemViewType(position);
            if (type == TYPE_HEADER) {
                holder.mBornView.setText(XStringUtil.calendar2str(mBornCal, "."));
            } else if (type == TYPE_NODE) {
                final Memory item = mValues.get(position - 1);
                if (true) {
                    holder.mAgeView.setText(String.valueOf(item.age));
                    holder.mMemoryNameView.setText(item.name);
                    holder.mAgeLayout.setVisibility(View.VISIBLE);
                }else {
                    holder.mAgeLayout.setVisibility(View.GONE);
                }
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                calendar.setTimeInMillis(item.happenTime);
                calendar.setTimeZone(TimeZone.getDefault());
                holder.mMemoryDateView.setText(XStringUtil.calendar2str(calendar, "."));
                if (item.secrets != null && item.secrets.size() > 0) {
                    holder.mMemoryImageLayout.setVisibility(View.VISIBLE);
//                    holder.mMemoryImage// TODO 加载图片
                } else {
                    holder.mMemoryImageLayout.setVisibility(View.GONE);
                }
                holder.mMemoryLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "点击Memory:" + item.name, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size() + 2;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public TextView mBornView;
            public View mAgeLayout;
            public TextView mAgeView;
            public TextView mAgeDesView;
            public View mMemoryLayout;
            public TextView mMemoryNameView;
            public TextView mMemoryDateView;
            public View mMemoryImageLayout;
            public ImageView mMemoryImage;

            public ViewHolder(View view, int viewType) {
                super(view);
                mView = view;
                if (viewType == TYPE_HEADER) {
                    mBornView = (TextView) view.findViewById(R.id.timeline_header_time);
                } else if (viewType == TYPE_NODE){
                    mAgeLayout = view.findViewById(R.id.timeline_tag_layout);
                    mAgeView = (TextView) view.findViewById(R.id.timeline_tag);
                    mAgeDesView = (TextView) view.findViewById(R.id.timeline_tag_txt);
                    mMemoryLayout = view.findViewById(R.id.timeline_node_layout);
                    mMemoryNameView = (TextView) view.findViewById(R.id.timeline_node_txt);
                    mMemoryDateView = (TextView) view.findViewById(R.id.timeline_node_date);
                    mMemoryImageLayout = view.findViewById(R.id.timeline_node_img_layout);
                    mMemoryImage = (ImageView) view.findViewById(R.id.timeline_node_img);
                }
            }
        }
    }
}
