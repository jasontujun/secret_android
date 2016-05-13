package me.buryinmind.android.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.DialogFragment;
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
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.tj.xengine.android.data.listener.XHandlerIdDataSourceListener;
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.dialog.AddMemoryDialog;
import me.buryinmind.android.app.dialog.ConfirmDialog;
import me.buryinmind.android.app.dialog.DialogListener;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.uicontrol.ParticleLayout;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.CircleTransform;
import me.buryinmind.android.app.util.TimeUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BIM_MainActivity";
    public static final String TAG_DATE_PICKER = "datepicker";

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

    private boolean mListTop = true;
    private boolean mCollapsed = false;

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
                // 已经滑到顶部，不能再向上滑了
                if (!ViewCompat.canScrollVertically(recyclerView, -1)) {
                    if (!mListTop) {
                        mListTop = true;
                        mAppBar.setExpanded(true, true);
                        XLog.d(TAG, "mTimelineView scroll to top! expand appbar!");
                    } else {
                        XLog.d(TAG, "mTimelineView already scroll to top! not expand appbar!");
                    }
                } else {
                    mListTop = false;
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
                if (!mAddingMemory) {
                    mAddingMemory = true;
                    Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate_start);
                    animation.setFillAfter(true);
                    mAddBtn.startAnimation(animation);
                    AddMemoryDialog.newInstance(new DialogListener() {
                        @Override
                        public void onDone(Object... result) {
                            String name = (String) result[0];
                            Calendar date = (Calendar) result[1];
                            // 统一改成GMT时区,再上传服务器
                            date.setTimeZone(TimeZone.getTimeZone("GMT"));
                            addMemory(name, date.getTimeInMillis());
                        }

                        @Override
                        public void onDismiss() {
                            mAddingMemory = false;
                            Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate_back);
                            animation.setFillAfter(true);
                            mAddBtn.startAnimation(animation);
                        }
                    }).show(getSupportFragmentManager(), AddMemoryDialog.TAG);
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
                mAdapter.addData(pos, memory);
                mTimelineView.scrollToPosition(pos + 1);
            }


            @Override
            public void onAddAllInUI(List<Memory> list) {
                XListIdDataSourceImpl<Memory> memorySource = (XListIdDataSourceImpl<Memory>)
                        XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
                mAdapter.setData(memorySource.copyAll());
            }

            @Override
            public void onDeleteInUI(Memory memory) {
                mAdapter.deleteData(memory);
            }

            @Override
            public void onDeleteAllInUI(List<Memory> list) {
                XListIdDataSourceImpl<Memory> memorySource = (XListIdDataSourceImpl<Memory>)
                        XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
                mAdapter.setData(memorySource.copyAll());
            }
        });
        // init first ui
        if (user.bornTime == 0) {
            // 让用户设置出生日期,再生成时间线
            mBirthdayView.setVisibility(View.VISIBLE);
            mBirthdayBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePicker(Calendar.getInstance(), new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                            Calendar birthCal = Calendar.getInstance();
                            birthCal.set(year, month, day);
                            // 统一改成GMT时区,再上传服务器
                            birthCal.setTimeZone(TimeZone.getTimeZone("GMT"));
                            final long bornTime = birthCal.getTimeInMillis();
                            updateBornTime(user.uid, bornTime, new ApiUtil.SimpleListener() {
                                @Override
                                public void onResult(boolean result) {
                                    if (result) {
                                        user.bornTime = bornTime;
                                        // 生成时间线
                                        showTimeline();
                                    }
                                }
                            });
                        }
                    });
                }
            });
            mTimelineView.setVisibility(View.GONE);
            mAddBtn.setVisibility(View.GONE);
        } else {
            // 生成时间线
            showTimeline();
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

    private DialogFragment showDatePicker(Calendar initCal, DatePickerDialog.OnDateSetListener listener) {
        if (initCal == null) {
            initCal = Calendar.getInstance();
        }
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(listener,
                initCal.get(Calendar.YEAR), initCal.get(Calendar.MONTH),
                initCal.get(Calendar.DAY_OF_MONTH), false);
        datePickerDialog.setYearRange(1902, Calendar.getInstance().get(Calendar.YEAR));
        datePickerDialog.show(getSupportFragmentManager(), TAG_DATE_PICKER);
        return datePickerDialog;
    }

    private void showTimeline() {
        // 生成时间线
        mBirthdayView.setVisibility(View.GONE);
        mTimelineView.setVisibility(View.VISIBLE);
        mAddBtn.setVisibility(View.VISIBLE);
        // init list adapter
        XListIdDataSourceImpl<Memory> memorySource = (XListIdDataSourceImpl<Memory>)
                XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
        mAdapter = new TimelineAdapter(memorySource.copyAll());
        mTimelineView.setAdapter(mAdapter);
        mTimelineView.setItemAnimator(new DefaultItemAnimator());
        // request memory data
        requestMemoryList();
    }

    private void requestMemoryList() {
        if (mWaiting)
            return;
        mWaiting = true;
        showProgress(true);
        User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        MyApplication.getAsyncHttp().execute(
                ApiUtil.getMemoryList(user.uid),
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
                        source.sort(Memory.comparator);
                    }
                });
    }

    private void addMemory(String memoryName, long happenTime) {
        if (mWaiting)
            return;
        mWaiting = true;
        User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        MyApplication.getAsyncHttp().execute(
                ApiUtil.addMemory(user.uid, memoryName, happenTime),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        Toast.makeText(MainActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(MainActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jsonObject) {
                        mWaiting = false;
                        Memory memory = Memory.fromJson(jsonObject);
                        if (memory != null) {
                            XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                                    XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
                            source.add(memory);
                            source.sort(Memory.comparator);
                        }
                    }
                }
        );
    }

    private void deleteMemory(final Memory memory) {
        if (mWaiting)
            return;
        mWaiting = true;
        User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        MyApplication.getAsyncHttp().execute(
                ApiUtil.deleteMemory(user.uid, memory.mid),
                new XAsyncHttp.Listener() {
                    @Override
                    public void onNetworkError() {
                        XLog.d(TAG, "deleteMemory onNetworkError()! mid=" + memory.mid);
                        mWaiting = false;
                        Toast.makeText(MainActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                        mAdapter.resetData(memory);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        XLog.d(TAG, "deleteMemory onFinishError()! mid=" + memory.mid);
                        mWaiting = false;
                        Toast.makeText(MainActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        mAdapter.resetData(memory);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object obj) {
                        XLog.d(TAG, "deleteMemory onFinishSuccess()! mid=" + memory.mid);
                        mWaiting = false;
                        XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                                XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
                        source.deleteById(memory.mid);
                    }
                }
        );
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

        private Map<Integer, Memory> mAges;
        private List<Memory> mValues;
        private List<Memory> mToBeDelete;

        public TimelineAdapter(List<Memory> items) {
            mAges = new HashMap<Integer, Memory>();
            mToBeDelete = new ArrayList<Memory>();
            setData(items);
        }

        public void setData(final List<Memory> memories) {
            mValues = memories;
            mToBeDelete.clear();
            mAges.clear();
            for (Memory memory : memories) {
                if (!mAges.containsKey(memory.age)) {
                    mAges.put(memory.age, memory);
                }
            }
            notifyDataSetChanged();
        }

        public void resetData(Memory memory) {
            mToBeDelete.remove(memory);
            int pos = mValues.indexOf(memory);
            if (pos == -1) {
                return;
            }
            notifyItemChanged(pos + 1);
        }

        public void addData(int pos, Memory memory) {
            mValues.add(pos, memory);
            if (!mAges.containsKey(memory.age)) {
                mAges.put(memory.age, memory);
            } else {
                // 如果新增的memory比同样age的memory早，则替换
                Memory m = mAges.get(memory.age);
                if (Memory.comparator.compare(memory, m) < 0) {
                    mAges.put(memory.age, memory);
                }
            }
            notifyItemInserted(pos + 1);
            notifyItemRangeChanged(pos + 1, Math.min(2, mValues.size() - (pos + 1)));
        }

        public void deleteData(Memory memory) {
            int pos = mValues.indexOf(memory);
            if (pos == -1) {
                return;
            }
            mValues.remove(memory);
            mToBeDelete.remove(memory);
            if (memory.equals(mAges.get(memory.age))) {
                boolean needRemove = true;
                for (Memory item : mValues) {
                    if (item.age == memory.age) {
                        mAges.put(item.age, item);
                        needRemove = false;
                        break;
                    }
                }
                if (needRemove) {
                    mAges.remove(memory.age);
                }
            }
            notifyItemRemoved(pos + 1);
            notifyItemRangeChanged(pos + 1, Math.min(2, mValues.size() - (pos + 1)));
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
                holder.mBornLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
                        final User user = source.getUser();
                        Calendar lastBornTime = TimeUtil.getCalendar(user.bornTime);
                        showDatePicker(lastBornTime, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                                Calendar birthCal = Calendar.getInstance();
                                birthCal.set(year, month, day);
                                // 统一改成GMT时区,再上传服务器
                                birthCal.setTimeZone(TimeZone.getTimeZone("GMT"));
                                final long bornTime = birthCal.getTimeInMillis();
                                updateBornTime(user.uid, bornTime, new ApiUtil.SimpleListener() {
                                    @Override
                                    public void onResult(boolean result) {
                                        if (result) {
                                            user.bornTime = bornTime;
                                            mAdapter.notifyDataSetChanged();
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
                GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
                User user = source.getUser();
                holder.mBornView.setText(XStringUtil.calendar2str(TimeUtil.getCalendar(user.bornTime), "."));
            } else if (type == TYPE_FOOTER) {
                holder.mFooterView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAddBtn.performClick();
                    }
                });
            } else {
                final Memory item = mValues.get(position - 1);
                if (mAges.get(item.age).equals(item)) {
                    holder.mAgeView.setText(String.valueOf(item.age));
                    holder.mAgeLayout.setVisibility(View.VISIBLE);
                }else {
                    holder.mAgeLayout.setVisibility(View.GONE);
                }
                if (mToBeDelete.contains(item)) {
                    // 正在删除的条目，不显示内容，也不能点击
                    holder.mMemoryLayout.hide();
                    holder.mMemoryLayout.setOnClickListener(null);
                } else {
                    holder.mMemoryNameView.setText(item.name);
                    holder.mMemoryDateView.setText(XStringUtil.calendar2str(TimeUtil.getCalendar(item.happenTime), "."));
                    if (item.secrets != null && item.secrets.size() > 0) {
                        holder.mMemoryImageLayout.setVisibility(View.VISIBLE);
//                    holder.mMemoryImage// TODO 加载图片
                    } else {
                        holder.mMemoryImageLayout.setVisibility(View.GONE);
                    }
                    holder.mMemoryLayout.setParticle(R.drawable.particle_star);
                    holder.mMemoryLayout.reset();
                    holder.mMemoryLayout.setListener(new ParticleLayout.Listener() {
                        @Override
                        public void onStart() {
                        }
                        @Override
                        public void onEnd() {
                            GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
                            final User user = source.getUser();
                            ConfirmDialog.newInstance(
                                    String.format(getResources().getString(R.string.info_delete_memory),
                                            TimeUtil.calculateAge(user.bornTime, item.happenTime),
                                            item.name),
                                    new DialogListener() {
                                        boolean confirm = false;
                                        @Override
                                        public void onDone(Object... result) {
                                            confirm = (boolean) result[0];
                                            if (confirm) {
                                                mToBeDelete.add(item);
                                                holder.mMemoryLayout.setOnClickListener(null);
                                                deleteMemory(item);
                                            }
                                        }
                                        @Override
                                        public void onDismiss() {
                                            if (!confirm) {
                                                holder.mMemoryLayout.reset();
                                            }
                                        }
                                    }).show(getSupportFragmentManager(), ConfirmDialog.TAG);
                        }
                        @Override
                        public void onCancelled() {
                        }
                    });
                    holder.mMemoryLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(MainActivity.this, "点击Memory:" + item.name, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size() + 2;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public View mBornLayout;
            public TextView mBornView;
            public View mAgeLayout;
            public TextView mAgeView;
            public TextView mAgeDesView;
            public ParticleLayout mMemoryLayout;
            public TextView mMemoryNameView;
            public TextView mMemoryDateView;
            public View mMemoryImageLayout;
            public ImageView mMemoryImage;
            public View mFooterView;

            public ViewHolder(View view, int viewType) {
                super(view);
                mView = view;
                if (viewType == TYPE_HEADER) {
                    mBornLayout = view.findViewById(R.id.timeline_header_layout);
                    mBornView = (TextView) view.findViewById(R.id.timeline_header_time);
                } else if (viewType == TYPE_FOOTER) {
                    mFooterView = view.findViewById(R.id.timeline_footer_txt);
                } else {
                    mAgeLayout = view.findViewById(R.id.timeline_tag_layout);
                    mAgeView = (TextView) view.findViewById(R.id.timeline_tag);
                    mAgeDesView = (TextView) view.findViewById(R.id.timeline_tag_txt);
                    mMemoryLayout = (ParticleLayout) view.findViewById(R.id.timeline_node_layout);
                    mMemoryNameView = (TextView) view.findViewById(R.id.timeline_node_txt);
                    mMemoryDateView = (TextView) view.findViewById(R.id.timeline_node_date);
                    mMemoryImageLayout = view.findViewById(R.id.timeline_node_img_layout);
                    mMemoryImage = (ImageView) view.findViewById(R.id.timeline_node_img);
                }
            }
        }
    }
}
