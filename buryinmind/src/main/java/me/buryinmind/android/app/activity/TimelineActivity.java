package me.buryinmind.android.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadOptions;
import com.tj.xengine.android.data.listener.XHandlerIdDataSourceListener;
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.android.utils.XStorageUtil;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.controller.ResultListener;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.dialog.AddMemoryDialog;
import me.buryinmind.android.app.dialog.ConfirmDialog;
import me.buryinmind.android.app.dialog.DialogListener;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.uicontrol.ParticleLayout;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.FileUtils;
import me.buryinmind.android.app.util.ImageUtil;
import me.buryinmind.android.app.util.TimeUtil;
import me.buryinmind.android.app.util.ViewUtil;

public class TimelineActivity extends AppCompatActivity {

    private static final String TAG = "BIM_MainActivity";
    public static final String TAG_DATE_PICKER = "datepicker";
    private static final int HEADER_REQUEST_CODE = 8090;
    private static final long MAX_IMAGE_SIZE = 100 * 1024;// 图片大小上限100K

    private View mProgressView;
    private View mContentView;
    private Toolbar mToolBar;
    private AppBarLayout mAppBar;
    private CollapsingToolbarLayout mCollapsedLayout;
    private ImageView mProfileBackground;
    private ImageView mAccountHeadView;
    private TextView mAccountNameView;
    private TextView mAccountDesView;
    private RecyclerView mTimelineView;
    private TimelineAdapter mAdapter;
    private View mAddBtn;
    private View mBirthdayView;

    private boolean mListTop = true;
    private boolean mCollapsed = false;

    private boolean mWaiting;
    private boolean mAddingMemory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        mProgressView = findViewById(R.id.loading_progress);
        mContentView = findViewById(R.id.content_layout);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
        mCollapsedLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mTimelineView = (RecyclerView) findViewById(R.id.timeline_list);
        mAddBtn = findViewById(R.id.timeline_add_btn);
        mBirthdayView = findViewById(R.id.timeline_birthday_layout);
        mProfileBackground = (ImageView) findViewById(R.id.account_profile_bg);
        mAccountHeadView = (ImageView) findViewById(R.id.account_head_img);
        mAccountNameView = (TextView) findViewById(R.id.account_name_txt);
        mAccountDesView = (TextView) findViewById(R.id.account_des_txt);

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

        // init add btn
        mAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickAddMemoryBtn(v);
            }
        });

        // init user profile
        final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        mAccountNameView.setText(user.name);
        mAccountDesView.setText(XStringUtil.list2String(user.descriptions, ", "));
        mAccountHeadView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickHead(v);
            }
        });
        mAccountDesView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickDes(v);
            }
        });
        showProfilePicture();

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
        inflater.inflate(R.menu.menu_timeline, menu);
        return true;
    }

    public void clickToolBar(View view) {
        if (mCollapsed) {
            mAppBar.setExpanded(true, true);
        } else {
            mAppBar.setExpanded(false, true);
        }
    }

    public void clickHead(View view) {
        // 选择图片，并上传
        Intent target = FileUtils.createGetContentIntent();
        Intent intent = Intent.createChooser(target, this.getString(R.string.info_choose_head));
        try {
            startActivityForResult(intent, HEADER_REQUEST_CODE);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, getString(R.string.error_select_picture),
                    Toast.LENGTH_LONG).show();
        }
    }

    public void clickDes(View view) {
        // TODO 修改描述
    }

    public void clickBirthBtn(View view) {
        final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        showDatePicker(Calendar.getInstance(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                Calendar birthCal = Calendar.getInstance();
                birthCal.set(year, month, day);
                // 统一改成GMT时区,再上传服务器
                birthCal.setTimeZone(TimeZone.getTimeZone("GMT"));
                final long bornTime = birthCal.getTimeInMillis();
                updateBornTime(user.uid, bornTime, new ResultListener() {
                    @Override
                    public void onResult(boolean result, Object data) {
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

    public void clickAddMemoryBtn(View view) {
        if (!mAddingMemory) {
            mAddingMemory = true;
            Animation animation = AnimationUtils.loadAnimation(TimelineActivity.this, R.anim.rotate_start);
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
                    Animation animation = AnimationUtils.loadAnimation(TimelineActivity.this, R.anim.rotate_back);
                    animation.setFillAfter(true);
                    mAddBtn.startAnimation(animation);
                }
            }).show(getSupportFragmentManager(), AddMemoryDialog.TAG);
        }
    }

    private void showProgress(final boolean show) {
        ViewUtil.animateFadeInOut(mContentView, show);
        ViewUtil.animateFadeInOut(mProgressView, !show);
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


    private void showProfilePicture() {
        final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        showProfilePicture(ApiUtil.getIdUrl(user.uid));
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
                        Toast.makeText(TimelineActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(TimelineActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(TimelineActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(TimelineActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(TimelineActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                        mAdapter.resetData(memory);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        XLog.d(TAG, "deleteMemory onFinishError()! mid=" + memory.mid);
                        mWaiting = false;
                        Toast.makeText(TimelineActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
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

    private void updateBornTime(String uid, long bornTime, final ResultListener listener) {
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
                        Toast.makeText(TimelineActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onResult(false, null);
                        }
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(TimelineActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onResult(false, null);
                        }
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                        mWaiting = false;
                        showProgress(false);
                        if (listener != null) {
                            listener.onResult(true, null);
                        }
                    }
                });
    }

    private void uploadHeadPicture(final String filePath, final boolean needDelete) {
        // 从业务服务器获取上传凭证
        final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        MyApplication.getAsyncHttp().execute(
                ApiUtil.getHeadToken(user.uid),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onNetworkError() {
                        Toast.makeText(TimelineActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        Toast.makeText(TimelineActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jsonObject) {
                        try {
                            final String token = jsonObject.getString("up");
                            // 真正开始上传
                            MyApplication.getUploadManager().put(filePath, user.uid, token,
                                    new UpCompletionHandler() {
                                        @Override
                                        public void complete(String key, ResponseInfo info, JSONObject response) {
                                            XLog.d(TAG, "upload complete! " + info.toString());
                                            if (info.isOK()) {
                                                XLog.d(TAG, "upload success!");
                                                MyApplication.updateImageTimestamp();// 更新图片时间戳
                                                showProfilePicture(filePath);
                                                if (needDelete) {
                                                    new File(filePath).deleteOnExit();
                                                }
                                            } else {
                                                Toast.makeText(TimelineActivity.this, R.string.error_upload_picture,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    },
                                    new UploadOptions(null, null, false,
                                            new UpProgressHandler() {
                                                public void progress(String key, double percent) {
                                                    XLog.d(TAG, "progress. " + key + ": " + percent);
                                                }
                                            }, null));
                        } catch (JSONException e) {
                            Toast.makeText(TimelineActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case HEADER_REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK && data != null) {
                    // Get the URI of the selected file
                    final Uri uri = data.getData();
                    try {
                        // Get the file path from the URI
                        final String path = FileUtils.getPath(this, uri);
                        if (path == null) {
                            Toast.makeText(this, getString(R.string.error_select_picture),
                                    Toast.LENGTH_LONG).show();
                            break;
                        }
                        File file = new File(path);
                        long fileSize = file.length();
                        if (fileSize > MAX_IMAGE_SIZE) {
                            // 先进行压缩再上传
                            XLog.d(TAG, "need compress! size=" + fileSize);
                            new AsyncTask<Void, Void, String>() {
                                @Override
                                protected String doInBackground(Void... params) {
                                    File dir = getCacheDir();
                                    if (dir != null && dir.exists() &&
                                            !XStorageUtil.isFull(dir.getAbsolutePath(), MAX_IMAGE_SIZE)) {
                                        File tmpFile = new File(dir, "tmp_" + System.currentTimeMillis() + ".jpg");
                                        if (ImageUtil.compress(TimelineActivity.this, path, tmpFile.getAbsolutePath(), MAX_IMAGE_SIZE)) {
                                            XLog.d(TAG, "head picture compress success!");
                                            return tmpFile.getAbsolutePath();
                                        } else {
                                            XLog.d(TAG, "head picture compress failed!");
                                            return null;
                                        }
                                    }
                                    return null;
                                }
                                @Override
                                protected void onPostExecute(String result) {
                                    if (!XStringUtil.isEmpty(result)) {
                                        uploadHeadPicture(result, true);
                                    } else {
                                        Toast.makeText(TimelineActivity.this, getString(R.string.error_select_picture),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            }.execute();
                        } else {
                            // 大小没超过上限，直接上传
                            XLog.d(TAG, "no need compress! size=" + fileSize);
                            uploadHeadPicture(path, false);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.error_select_picture),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

        private static final int TYPE_HEADER = 1;
        private static final int TYPE_FOOTER = 2;
        private static final int TYPE_NODE = 3;

        private Map<Integer, Memory> mAges;
        private List<Memory> mItems;
        private List<Memory> mToBeDelete;

        public TimelineAdapter(List<Memory> items) {
            mAges = new HashMap<Integer, Memory>();
            mToBeDelete = new ArrayList<Memory>();
            setData(items);
        }

        public void setData(final List<Memory> memories) {
            mItems = memories;
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
            int pos = mItems.indexOf(memory);
            if (pos == -1) {
                return;
            }
            notifyItemChanged(pos + 1);
        }

        public void addData(int pos, Memory memory) {
            mItems.add(pos, memory);
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
            notifyItemRangeChanged(pos + 1, Math.min(2, mItems.size() - (pos + 1)));
        }

        public void deleteData(Memory memory) {
            int pos = mItems.indexOf(memory);
            if (pos == -1) {
                return;
            }
            mItems.remove(memory);
            mToBeDelete.remove(memory);
            if (memory.equals(mAges.get(memory.age))) {
                boolean needRemove = true;
                for (Memory item : mItems) {
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
            notifyItemRangeChanged(pos + 1, Math.min(2, mItems.size() - (pos + 1)));
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return TYPE_HEADER;
            else if (position == mItems.size() + 1)
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
                                updateBornTime(user.uid, bornTime, new ResultListener() {
                                    @Override
                                    public void onResult(boolean result, Object data) {
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
                final Memory item = mItems.get(position - 1);
                if (item.age > 0 && mAges.get(item.age).equals(item)) {
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
                            Toast.makeText(TimelineActivity.this, "点击Memory:" + item.name, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(TimelineActivity.this, MemoryDetailActivity.class);
                            intent.putExtra("mid", item.mid);
                            startActivity(intent);
                        }
                    });
                }
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size() + 2;
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
