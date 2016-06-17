package me.buryinmind.android.app.fragment;

import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
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
import java.util.Collections;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.controller.ProgressListener;
import me.buryinmind.android.app.data.SecretSource;
import me.buryinmind.android.app.dialog.ConfirmDialog;
import me.buryinmind.android.app.dialog.DialogListener;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.MemoryGift;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.uicontrol.XLinearLayoutManager;
import me.buryinmind.android.app.adapter.XListAdapter;
import me.buryinmind.android.app.adapter.XViewHolder;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/6/8.
 */
public class MemoryDetailFragment extends XFragment {

    private static final String TAG = MemoryDetailFragment.class.getSimpleName();
    public static final String KEY_MID = "mid";
    public static final int REFRESH_COLLAPSE = 10;
    public static final int REFRESH_EXPAND = 11;
    public static final int REFRESH_MENU = 12;

    private View mProgressView;
    private RecyclerView mSecretListView;
    private EditAdapter mSecretAdapter;
    private ItemTouchHelper mItemTouchHelper;

    private Memory mMemory;
    private boolean mListTop = true;
    private boolean mAutoExpand = true;
    private int mScreenWidth;
    private boolean mWaiting;
    private boolean mReorder;// 是否需要同步secret的顺序

    @Override
    public void onCreate(Bundle savedInstanceState) {
        XLog.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        Bundle argument = getArguments();
        if (argument != null) {
            String memoryId = argument.getString(KEY_MID);
            XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                    XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
            mMemory = source.getById(memoryId);
        }
        mSecretAdapter = new EditAdapter(mMemory.secrets);
        // 在onCreate时请求数据，可以避免fragment切换时过于频繁请求
        requestDetail();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        XLog.d(TAG, "onCreateView()");
        DisplayMetrics outMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;

        View rootView = inflater.inflate(R.layout.fragment_secret_list, container, false);
        mProgressView = rootView.findViewById(R.id.loading_progress);
        mSecretListView = (RecyclerView) rootView.findViewById(R.id.memory_secret_list);

        // init recycler view
        mSecretListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                        if (mAutoExpand) {
                            notifyRefresh(REFRESH_EXPAND, null);
                        }
                        XLog.d(TAG, "mSecretListView scroll to top! expand appbar!");
                    } else {
                        XLog.d(TAG, "mSecretListView already scroll to top! not expand appbar!");
                    }
                } else {
                    mListTop = false;
                }
            }
        });
        mSecretListView.setLayoutManager(new XLinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false));
        mSecretListView.setAdapter(mSecretAdapter);
        ItemTouchHelper.Callback mCallback = new ItemTouchHelper.SimpleCallback
                (ItemTouchHelper.UP|ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                XLog.d(TAG, "onMove");
                int fromPosition = viewHolder.getAdapterPosition();//得到拖动ViewHolder的position
                int toPosition = target.getAdapterPosition();//得到目标ViewHolder的position
                if (fromPosition < toPosition) {
                    //分别把中间所有的item的位置重新交换
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(mMemory.secrets, i, i + 1);
                        Collections.swap(mSecretAdapter.getData(), i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(mMemory.secrets, i, i - 1);
                        Collections.swap(mSecretAdapter.getData(), i, i - 1);
                    }
                }
                mSecretAdapter.notifyItemMoved(fromPosition, toPosition);
                // 重新设定order值
                for (int i = 0; i < mMemory.secrets.size(); i++) {
                    mMemory.secrets.get(i).order = i;
                }
                mReorder = true;
                return true;//返回true表示执行拖动
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                XLog.d(TAG, "onSwiped, direction=" + direction);
                ConfirmDialog.newInstance(
                        getResources().getString(R.string.info_delete_secret),
                        new DialogListener() {
                            boolean confirm = false;

                            @Override
                            public void onDone(Object... result) {
                                confirm = (boolean) result[0];
                                if (confirm) {
                                    XViewHolder holder = (XViewHolder) viewHolder;
                                    Secret secret = (Secret) holder.getData();
                                    deleteSecret(secret);
                                }
                            }

                            @Override
                            public void onDismiss() {
                                if (!confirm) {
                                    XViewHolder holder = (XViewHolder) viewHolder;
                                    Secret secret = (Secret) holder.getData();
                                    mSecretAdapter.refreshData(secret, null);
                                }
                            }
                        }).show(getFragmentManager(), ConfirmDialog.TAG);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }


            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    //滑动时改变Item的透明度
                    final float alpha = 1 - Math.abs(dX) / (float)viewHolder.itemView.getWidth();
                    viewHolder.itemView.setAlpha(alpha);
                    viewHolder.itemView.setTranslationX(dX);
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder,
                                          int actionState) {
                XLog.d(TAG, "onSelectedChanged, actionState=" + actionState);
                // item拖拽被触发时
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    mAutoExpand = false;
                    notifyRefresh(REFRESH_COLLAPSE, null);// 通知activity收缩起来
                    XViewHolder holder = (XViewHolder) viewHolder;
                    holder.getView(R.id.secret_item_cover_layout).setVisibility(View.VISIBLE);
                }
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void clearView(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder) {
                XLog.d(TAG, "clearView");
                super.clearView(recyclerView, viewHolder);
                mAutoExpand = true;
                XViewHolder holder = (XViewHolder) viewHolder;
                holder.getView(R.id.secret_item_cover_layout).setVisibility(View.GONE);
                holder.itemView.setAlpha(1);
            }
        };
        mItemTouchHelper = new ItemTouchHelper(mCallback);
        mItemTouchHelper.attachToRecyclerView(mSecretListView);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        XLog.d(TAG, "onStart()");
    }

    @Override
    public void onStop() {
        super.onStart();
        XLog.d(TAG, "onStop()");
    }

    @Override
    public void onDestroyView() {
        XLog.d(TAG, "onDestroyView()");
        super.onDestroyView();
        // 退出此界面时，同步secret顺序
        reorderSecret();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XLog.d(TAG, "onStart()");
    }

    public void needScrollToTop() {
        mSecretListView.smoothScrollToPosition(0);
    }

    private void showProgress(final boolean show) {
        ViewUtil.animateFadeInOut(mSecretListView, show);
        ViewUtil.animateFadeInOut(mProgressView, !show);
    }

    private boolean requestDetail() {
        if (mWaiting)
            return false;
        mWaiting = true;
        showProgress(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.getMemoryDetail(mMemory.mid),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jo) {
                        mWaiting = false;
                        showProgress(false);
                        List<Secret> secrets = null;
                        if (jo.has("gifts") && !jo.isNull("gifts")) {
                            try {
                                JSONArray giftArr = jo.getJSONArray("gifts");
                                mMemory.outGifts = MemoryGift.fromJson(giftArr);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (jo.has("secrets") && !jo.isNull("secrets")) {
                            try {
                                JSONArray secretArr = jo.getJSONArray("secrets");
                                secrets = Secret.fromJson(secretArr);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        XLog.d(TAG, "详情，secrets:" + (secrets == null ? 0 : secrets.size()));
                        if (secrets != null && secrets.size() > 0) {
                            SecretSource source = (SecretSource) XDefaultDataRepo.getInstance()
                                    .getSource(MyApplication.SOURCE_SECRET);
                            source.addAll(secrets);
                            mMemory.secrets = source.getByMemoryId(mMemory.mid);
                            notifyRefresh(REFRESH_MENU, null);
                            mSecretAdapter.setData(mMemory.secrets);
                            // 刷新列表
                            for (Secret secret : mMemory.secrets) {
                                // Secret文件已下载，直接显示
                                if (!XStringUtil.isEmpty(secret.localPath)) {
                                    File file = new File(secret.localPath);
                                    if (file.exists() && file.length() == secret.size) {
                                        mSecretAdapter.refreshData(secret, null);
                                        continue;
                                    }
                                }
                                // 否则，重新下载
                                MyApplication.getSecretDownloader().download(secret,
                                        new ProgressListener<Secret>() {
                                            @Override
                                            public void onProgress(Secret secret,
                                                                   long completeSize,
                                                                   long totalSize) {
                                                mSecretAdapter.refreshData(secret, new Object());
                                            }

                                            @Override
                                            public void onResult(boolean result, Secret secret) {
                                                if (result) {
                                                    // 本地缓存文件路径，同步到数据库
                                                    SecretSource source = (SecretSource) XDefaultDataRepo.getInstance()
                                                            .getSource(MyApplication.SOURCE_SECRET);
                                                    source.saveToDatabase(secret);
                                                    // 局部刷新列表的对应一项
                                                    XLog.d(TAG, "下载secret成功.id=" + secret.sid);
                                                    secret.completeSize = -1;
                                                    mSecretAdapter.refreshData(secret, null);
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });
        return true;
    }

    public void addSecret(final List<Secret> secrets) {
        showProgress(true);
        // 先在服务器端创建secret
        MyApplication.getAsyncHttp().execute(
                ApiUtil.addSecret(mMemory.mid, secrets),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
                    @Override
                    public void onNetworkError() {
                        showProgress(false);
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        showProgress(false);
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONArray jsonArray) {
                        showProgress(false);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                JSONObject jo = jsonArray.getJSONObject(i);
                                if (jo != null) {
                                    Secret se = secrets.get(i);
                                    se.sid = jo.getString("sid");
                                    se.dfs = jo.getInt("dfs");
                                    se.createTime = jo.getLong("ctime");
                                    se.setId(se.mid, se.sid);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        SecretSource source = (SecretSource) XDefaultDataRepo.getInstance()
                                .getSource(MyApplication.SOURCE_SECRET);
                        source.addAll(secrets);
                        mMemory.secrets = source.getByMemoryId(mMemory.mid);
                        notifyRefresh(REFRESH_MENU, null);
                        mSecretAdapter.addData(secrets);
                        // 再上传图片文件
                        for (Secret secret : secrets) {
                            MyApplication.getSecretUploader().upload(secret,
                                    new ProgressListener<Secret>() {
                                        @Override
                                        public void onProgress(Secret secret,
                                                               long completeSize,
                                                               long totalSize) {
                                            mSecretAdapter.refreshData(secret, new Object());
                                        }

                                        @Override
                                        public void onResult(boolean result, Secret secret) {
                                            if (!result) {
                                                Toast.makeText(getActivity(),
                                                        "上传Secret失败!", Toast.LENGTH_SHORT).show();
                                            }
                                            secret.completeSize = -1;
                                            mSecretAdapter.refreshData(secret, null);
                                        }
                                    });
                        }
                    }
                });
    }

    private boolean deleteSecret(final Secret secret) {
        if (mWaiting) {
            mSecretAdapter.refreshData(secret, null);
            return false;
        }
        mWaiting = true;
        MyApplication.getAsyncHttp().execute(
                ApiUtil.deleteSecret(mMemory.mid, secret.sid),
                new XAsyncHttp.Listener() {
                    @Override
                    public void onNetworkError() {
                        XLog.d(TAG, "deleteSecret onNetworkError()! sid=" + secret.sid);
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        mSecretAdapter.refreshData(secret, null);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        XLog.d(TAG, "deleteSecret onFinishError()! sid=" + secret.sid);
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        mSecretAdapter.refreshData(secret, null);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object obj) {
                        XLog.d(TAG, "deleteSecret onFinishSuccess()! sid=" + secret.sid);
                        mWaiting = false;
                        // 从缓存数据源中清除
                        SecretSource source = (SecretSource) XDefaultDataRepo.
                                getInstance().getSource(MyApplication.SOURCE_SECRET);
                        source.deleteById(secret.getId());
                        mMemory.secrets = source.getByMemoryId(mMemory.mid);
                        notifyRefresh(REFRESH_MENU, null);
                        // 删除本地缓存文件
                        if (!XStringUtil.isEmpty(secret.localPath)) {
                            File cacheFile = new File(secret.localPath);
                            if (cacheFile.exists()) {
                                cacheFile.deleteOnExit();
                            }
                        }
                        // 刷新列表
                        mSecretAdapter.deleteData(secret);
                    }
                }
        );
        return true;
    }

    private void reorderSecret() {
        if (!mReorder)
            return;
        MyApplication.getAsyncHttp().execute(
                ApiUtil.orderSecret(mMemory.mid, mMemory.secrets),
                new XAsyncHttp.Listener() {
                    @Override
                    public void onNetworkError() {
                        XLog.d(TAG, "reorderSecret onNetworkError()! mid=" + mMemory.mid);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        XLog.d(TAG, "reorderSecret onFinishError()! mid=" + mMemory.mid);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object obj) {
                        XLog.d(TAG, "reorderSecret onFinishSuccess()! mid=" + mMemory.mid);
                        mReorder = false;
                    }
                }
        );
    }

    private class EditAdapter extends XListAdapter<Secret> {
        public EditAdapter(List<Secret> items) {
            super(R.layout.item_edit_secret, items);
        }

        @Override
        public XViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new XViewHolder(LayoutInflater.from(getActivity())
                    .inflate(R.layout.item_edit_secret, parent, false));
        }

        @Override
        public void onBindViewHolder(XViewHolder holder, int position, List<Object> payload) {
            if (payload.size() == 0) {
                XLog.d(TAG, "onBindViewHolder(payload), payload.size=0.");
                onBindViewHolder(holder, position);
            } else {
                XLog.d(TAG, "onBindViewHolder(payload), payload.size=" + payload.size());
                final Secret item = getData().get(position);
                // set progress
                TextView progressView = (TextView) holder.getView(R.id.secret_item_progress);
                if (item.completeSize >= 0) {
                    float percent = item.size == 0 ? 0 : ((float)item.completeSize / (float)item.size);
                    progressView.setText(XStringUtil.percent2str(percent));
                    progressView.setVisibility(View.VISIBLE);
                } else {
                    progressView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onBindViewHolder(final XViewHolder holder, final int position) {
            XLog.d(TAG, "onBindViewHolder()");
            final Secret item = getData().get(position);
            holder.bindData(item);
            // set image
            View imageLayout = holder.getView(R.id.secret_item_img_layout);
            int imageViewWidth = mScreenWidth;
            int imageViewHeight = imageViewWidth  * item.height / item.width;
            ViewGroup.LayoutParams params = imageLayout.getLayoutParams();
            params.width = imageViewWidth;
            params.height = imageViewHeight;
            imageLayout.setLayoutParams(params);
            // show image
            ImageView imageView = (ImageView) holder.getView(R.id.secret_item_img);
            if (XStringUtil.isEmpty(item.localPath)) {
                imageView.setImageResource(R.color.darkGray);
            } else {
                Glide.with(MemoryDetailFragment.this)
                        .load(item.localPath)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.color.darkGray)
                        .into(imageView);
            }
            // set drag handle
            holder.getView(R.id.secret_item_handle).setOnTouchListener(
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (MotionEventCompat.getActionMasked(event) ==
                                    MotionEvent.ACTION_DOWN) {
                                mItemTouchHelper.startDrag(holder);
                            }
                            return false;
                        }
                    });
            // set progress
            TextView progressView = (TextView) holder.getView(R.id.secret_item_progress);
            if (item.completeSize >= 0) {
                float percent = item.size == 0 ? 0 : ((float)item.completeSize / (float)item.size);
                progressView.setText(XStringUtil.percent2str(percent));
                progressView.setVisibility(View.VISIBLE);
            } else {
                progressView.setVisibility(View.GONE);
            }
            // set click listener
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(),
                            "click image " + item.localPath, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
